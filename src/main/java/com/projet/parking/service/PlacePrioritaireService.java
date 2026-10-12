package com.projet.parking.service;

import com.projet.parking.dto.JustificatifDTO;
import com.projet.parking.entity.*;
import com.projet.parking.enumeration.*;
import com.projet.parking.exception.OperationNonAutoriseeException;
import com.projet.parking.exception.ResourceNotFoundException;
import com.projet.parking.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class PlacePrioritaireService {

    private static final List<StatutReservation> RESERVATIONS_ACTIVES =
            List.of(StatutReservation.EN_ATTENTE, StatutReservation.CONFIRMEE, StatutReservation.EN_COURS);
    private static final Duration HORIZON_CONVERSION = Duration.ofDays(30);  // pas de conversion si réservée avant

    // Réglables dans application.properties
    @Value("${parking.prioritaire.quota-minimum-pourcentage:2.0}")
    private double quotaPourcentage;
    @Value("${parking.prioritaire.reserve-libre:1}")
    private int reserveLibre;   // places prioritaires qui doivent rester libres pour les ayants droit

    private final PlacesRepository placesRepository;
    private final JustificatifPmrRepository justificatifRepository;
    private final ClientsRepository clientsRepository;
    private final ReglesSubstitutionRepository reglesRepository;
    private final EntreeSortiesRepository entreeSortiesRepository;
    private final AnomalieService anomalieService;

    public PlacePrioritaireService(PlacesRepository placesRepository,
                                   JustificatifPmrRepository justificatifRepository,
                                   ClientsRepository clientsRepository,
                                   ReglesSubstitutionRepository reglesRepository,
                                   EntreeSortiesRepository entreeSortiesRepository,
                                   AnomalieService anomalieService) {
        this.placesRepository = placesRepository;
        this.justificatifRepository = justificatifRepository;
        this.clientsRepository = clientsRepository;
        this.reglesRepository = reglesRepository;
        this.entreeSortiesRepository = entreeSortiesRepository;
        this.anomalieService = anomalieService;
    }

    // ===================================================================== JUSTIFICATIFS

    /** Dépôt de la carte PMR par le client : elle reste EN_ATTENTE jusqu'à vérification par un admin. */
    @Transactional
    public Long enregistrerJustificatif(Long userId, String numeroCarte, LocalDate dateExpiration) {
        Clients client = clientsRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "avec id inexistant : " + userId));
        if (numeroCarte == null || numeroCarte.isBlank()) {
            throw new OperationNonAutoriseeException("Le numéro de carte est obligatoire");
        }
        String numero = numeroCarte.trim().toUpperCase();
        if (justificatifRepository.existsByNumeroCarte(numero)) {
            throw new OperationNonAutoriseeException("Cette carte est déjà enregistrée");
        }
        if (dateExpiration != null && dateExpiration.isBefore(LocalDate.now())) {
            throw new OperationNonAutoriseeException("Cette carte est expirée");
        }
        JustificatifPmr j = new JustificatifPmr();
        j.setClients(client);
        j.setNumeroCarte(numero);
        j.setDateExpiration(dateExpiration);
        j.setStatut(StatutJustificatif.EN_ATTENTE);
        j.setDateDepot(LocalDateTime.now());
        return justificatifRepository.save(j).getJustificatif_id();
    }

    /** À réserver aux administrateurs (@PreAuthorize sur le contrôleur). */
    @Transactional
    public void validerJustificatif(Long id) {
        decider(id, StatutJustificatif.VERIFIE);
    }

    @Transactional
    public void refuserJustificatif(Long id) {
        decider(id, StatutJustificatif.REFUSE);
    }

    private void decider(Long id, StatutJustificatif decision) {
        JustificatifPmr j = justificatifRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Justificatif", "avec id inexistant : " + id));
        if (j.getStatut() != StatutJustificatif.EN_ATTENTE) {
            throw new OperationNonAutoriseeException("Justificatif déjà traité : " + j.getStatut());
        }
        j.setStatut(decision);
        j.setDateVerification(LocalDateTime.now());
    }

    // ===================================================================== ÉLIGIBILITÉ ET ATTRIBUTION

    public boolean estEligible(Clients client, Vehicules vehicule) {
        return motifRefus(client, vehicule) == null;
    }

    public void verifierEligibilite(Clients client, Vehicules vehicule) {
        String motif = motifRefus(client, vehicule);
        if (motif != null) {
            throw new OperationNonAutoriseeException("Place prioritaire refusée : " + motif);
        }
    }

    private String motifRefus(Clients client, Vehicules vehicule) {
        if (client == null) return "véhicule occasionnel sans compte client";
        if (client.getStatut() != StatutUtilisateur.ACTIF) return "compte non actif";
        if (vehicule == null || vehicule.getType_vehicule() != TypeVehicule.PMR) return "le véhicule n'est pas de type PMR";
        if (vehicule.getClients() == null || !vehicule.getClients().getClient_id().equals(client.getClient_id())) {
            return "le véhicule n'appartient pas à ce client";
        }
        if (!justificatifRepository.existsValide(client.getClient_id(), StatutJustificatif.VERIFIE, LocalDate.now())) {
            return "aucun justificatif PMR valide";
        }
        return null;
    }

    /** Première place PRM libre sur le créneau ; vide s'il n'y en a plus. Lève une exception si non ayant droit. */
    public Optional<Places> attribuerPlacePrioritaire(Vehicules vehicule, LocalDateTime debut, LocalDateTime fin) {
        verifierEligibilite(vehicule.getClients(), vehicule);
        return placesLibres(TypePlace.PRM, debut, fin).stream().findFirst();
    }

    // ===================================================================== QUOTA MINIMUM

    /**
     * Garantit qu'il existe au moins X % de places PRM (au minimum 1). Si elles manquent, des places
     * STANDARD libres (aux niveaux les plus bas) sont converties. Retourne le nombre de conversions.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public int garantirQuotaMinimum() {
        long exploitables = placesRepository.count() - placesRepository.compterParStatut(StatutPlace.HORS_SERVICE);
        if (exploitables == 0) {
            return 0;
        }
        long requis = Math.max(1, (long) Math.ceil(exploitables * quotaPourcentage / 100.0));
        long manque = requis - placesRepository.compterParTypeSaufStatut(TypePlace.PRM, StatutPlace.HORS_SERVICE);
        if (manque <= 0) {
            return 0;
        }

        LocalDateTime maintenant = LocalDateTime.now();
        List<Places> aConvertir = placesLibres(TypePlace.STANDARD, maintenant, maintenant.plus(HORIZON_CONVERSION))
                .stream()
                .sorted(Comparator.comparing(Places::getNiveau, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Places::getNumero))
                .limit(manque)
                .toList();
        aConvertir.forEach(p -> p.setType_place(TypePlace.PRM));

        if (aConvertir.size() < manque) {
            anomalieService.signaler(TypeAnomalie.AUTRE, "Quota de places prioritaires non atteint : "
                    + aConvertir.size() + " conversion(s) sur " + manque + " nécessaire(s)");
        }
        return aConvertir.size();
    }

    // ===================================================================== REPLI

    /**
     * Parking plein pour les places STANDARD : applique les règles de ReglesSubstitution (STANDARD -> substitut,
     * par ordre de priorité). Les places PRM ne sont données que si plus de `reserveLibre` restent libres.
     */
    @Transactional(readOnly = true)
    public Optional<Places> appliquerRegleRepli(Vehicules vehicule, LocalDateTime debut, LocalDateTime fin) {
        if (!placesLibres(TypePlace.STANDARD, debut, fin).isEmpty()) {
            return Optional.empty();   // le parking n'est pas plein : pas de repli
        }
        for (ReglesSubstitution regle : reglesRepository
                .findByTypePlaceDemandeOrderByOrdrePrioriteAsc(TypePlace.STANDARD)) {
            List<Places> libres = placesLibres(regle.getTypePlaceSubstitut(), debut, fin);
            int reserve = regle.getTypePlaceSubstitut() == TypePlace.PRM ? reserveLibre : 0;
            if (libres.size() > reserve) {
                return Optional.of(libres.get(0));
            }
        }
        return Optional.empty();
    }

    // ===================================================================== OCCUPATION ILLÉGITIME

    /** Place PRM occupée par un véhicule qui n'a pas (ou plus) le droit : une anomalie est générée une seule fois. */
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public int detecterOccupationIllegitime() {
        int alertes = 0;
        for (EntreeSorties s : entreeSortiesRepository.findOccupationsNonSignalees(TypePlace.PRM)) {
            Vehicules v = s.getVehicules();
            if (!estEligible(v.getClients(), v)) {
                anomalieService.signaler(TypeAnomalie.OCCUPATION_ILLEGITIME,
                        "Place prioritaire " + s.getPlaces().getNumero() + " occupée par "
                                + v.getImmatriculation() + " (session " + s.getEntreeSortie_id() + ")");
                s.setOccupationSignalee(true);
                alertes++;
            }
        }
        return alertes;
    }

    // ===================================================================== OUTIL

    private List<Places> placesLibres(TypePlace type, LocalDateTime debut, LocalDateTime fin) {
        return placesRepository.trouverDisponibles(List.of(type), StatutPlace.HORS_SERVICE,
                        RESERVATIONS_ACTIVES, debut, fin)
                .stream()
                .filter(p -> p.getStatut() == StatutPlace.LIBRE)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<JustificatifDTO> getJustificatifsEnAttente() {
        return justificatifRepository.findByStatut(StatutJustificatif.EN_ATTENTE).stream().map(this::versDto).toList();
    }

    @Transactional(readOnly = true)
    public List<JustificatifDTO> getJustificatifsParUtilisateur(Long userId) {
        return justificatifRepository.findByClientId(userId).stream().map(this::versDto).toList();
    }

    private JustificatifDTO versDto(JustificatifPmr j) {
        return new JustificatifDTO(j.getJustificatif_id(), j.getNumeroCarte(), j.getDateExpiration(), j.getStatut(),
                j.getClients().getClient_id(), j.getClients().getNom() + " " + j.getClients().getPrenom(),
                j.getDateDepot());
    }
}
