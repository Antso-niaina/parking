package com.projet.parking.service;

import com.projet.parking.dto.AbonnementsDTO;
import com.projet.parking.entity.Abonnements;
import com.projet.parking.entity.Clients;
import com.projet.parking.entity.Places;
import com.projet.parking.entity.Vehicules;
import com.projet.parking.enumeration.*;
import com.projet.parking.exception.OperationNonAutoriseeException;
import com.projet.parking.exception.ResourceNotFoundException;
import com.projet.parking.mapper.AbonnementsMapper;
import com.projet.parking.repository.AbonnementsRepository;
import com.projet.parking.repository.ClientsRepository;
import com.projet.parking.repository.PlacesRepository;
import com.projet.parking.repository.VehiculesRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class AbonnementService {


    private static final List<StatutAbonnement> STATUTS_BLOQUANTS =
            List.of(StatutAbonnement.EN_ATTENTE, StatutAbonnement.ACTIF,
                    StatutAbonnement.SUSPENDU, StatutAbonnement.RENOUVELE);

    private static final List<StatutAbonnement> STATUTS_EN_COURS =
            List.of(StatutAbonnement.ACTIF, StatutAbonnement.RENOUVELE);
    private static final List<StatutAbonnement> STATUTS_EXPIRABLES =
            List.of(StatutAbonnement.ACTIF, StatutAbonnement.RENOUVELE, StatutAbonnement.SUSPENDU);

    private static final LocalTime DEBUT_JOUR = LocalTime.of(6, 0);
    private static final LocalTime FIN_JOUR = LocalTime.of(20, 0);
    private static final Duration TOLERANCE_DEBUT = Duration.ofMinutes(5);
    private static final Duration DELAI_PAIEMENT = Duration.ofHours(48);
    private static final Duration PREAVIS_ALERTE = Duration.ofDays(7);
    private static final DateTimeFormatter FORMAT_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    private final AbonnementsRepository abonnementsRepository;
    private final ClientsRepository clientsRepository;
    private final VehiculesRepository vehiculesRepository;
    private final PlacesRepository placesRepository;
    private final ClientService clientService;
    private final AlerteService alerteService;

    public AbonnementService(AbonnementsRepository abonnementsRepository,
                             ClientsRepository clientsRepository,
                             VehiculesRepository vehiculesRepository,
                             PlacesRepository placesRepository,
                             ClientService clientService,
                             AlerteService alerteService) {
        this.abonnementsRepository = abonnementsRepository;
        this.clientsRepository = clientsRepository;
        this.vehiculesRepository = vehiculesRepository;
        this.placesRepository = placesRepository;
        this.clientService = clientService;
        this.alerteService = alerteService;
    }



    @Transactional
    public AbonnementsDTO souscrire(Long userId, List<Long> vehiculeIds, FormuleAbonnement formule,
                                    LocalDateTime dateDebut, TypeAbonnements duree,
                                    boolean renouvellementAuto) {
        if (formule == null || duree == null || dateDebut == null) {
            throw new OperationNonAutoriseeException("Formule, durée et date de début sont obligatoires");
        }
        if (vehiculeIds == null || vehiculeIds.isEmpty()) {
            throw new OperationNonAutoriseeException("Au moins un véhicule est obligatoire");
        }
        if (dateDebut.isBefore(LocalDateTime.now().minus(TOLERANCE_DEBUT))) {
            throw new OperationNonAutoriseeException("La date de début est déjà passée");
        }

        Clients client = trouverClientActif(userId);
        clientService.verifierImpayes(userId);
        List<Vehicules> vehicules = chargerVehicules(userId, vehiculeIds);

        LocalDateTime dateFin = calculerFin(dateDebut, duree);
        verifierAbsenceChevauchement(vehicules, dateDebut, dateFin);
        verifierQuota(vehicules.size(), dateDebut, dateFin);

        Abonnements a = nouvelAbonnement(client, vehicules, formule, duree, dateDebut, dateFin, renouvellementAuto);
        return AbonnementsMapper.toDto(abonnementsRepository.save(a));
    }


    @Transactional
    public AbonnementsDTO confirmerPaiement(Long id, BigDecimal montant) {
        Abonnements a = trouverEntite(id);
        if (a.getStatutAbonnement() != StatutAbonnement.EN_ATTENTE) {
            throw new OperationNonAutoriseeException("Paiement impossible : abonnement " + a.getStatutAbonnement());
        }
        if (montant == null || montant.signum() <= 0) {
            throw new OperationNonAutoriseeException("Le montant payé est obligatoire");
        }
        a.setMontantPaye(montant);
        a.setStatutPaiement(StatutPaiement.PAYEE);
        a.setStatutAbonnement(StatutAbonnement.ACTIF);
        return AbonnementsMapper.toDto(a);
    }




    @Transactional
    public AbonnementsDTO renouveler(Long id) {
        Abonnements ancien = trouverEntite(id);
        if (ancien.getStatutAbonnement() != StatutAbonnement.ACTIF) {
            throw new OperationNonAutoriseeException("Renouvellement impossible : abonnement " + ancien.getStatutAbonnement());
        }
        trouverClientActif(ancien.getClients().getClient_id());
        return AbonnementsMapper.toDto(creerRenouvellement(ancien, LocalDateTime.now()));
    }

    private Abonnements creerRenouvellement(Abonnements ancien, LocalDateTime maintenant) {

        LocalDateTime debut = ancien.getDate_fin().isAfter(maintenant) ? ancien.getDate_fin() : maintenant;
        LocalDateTime fin = calculerFin(debut, ancien.getType_abonnements());
        List<Vehicules> vehicules = new ArrayList<>(ancien.getVehicules());

        verifierAbsenceChevauchement(vehicules, debut, fin);
        verifierQuota(vehicules.size(), debut, fin);

        Abonnements nouveau = nouvelAbonnement(ancien.getClients(), vehicules, ancien.getFormule(),
                ancien.getType_abonnements(), debut, fin, ancien.isRenouvellementAuto());
        ancien.setStatutAbonnement(StatutAbonnement.RENOUVELE);
        return abonnementsRepository.save(nouveau);
    }



    @Transactional
    public AbonnementsDTO suspendre(Long id) {
        Abonnements a = trouverEntite(id);
        if (a.getStatutAbonnement() != StatutAbonnement.ACTIF) {
            throw new OperationNonAutoriseeException("Suspension impossible : abonnement " + a.getStatutAbonnement());
        }
        a.setStatutAbonnement(StatutAbonnement.SUSPENDU);
        return AbonnementsMapper.toDto(a);
    }


    @Transactional
    public BigDecimal resilier(Long id) {
        Abonnements a = trouverEntite(id);
        if (a.getStatutAbonnement() != StatutAbonnement.EN_ATTENTE
                && a.getStatutAbonnement() != StatutAbonnement.ACTIF
                && a.getStatutAbonnement() != StatutAbonnement.SUSPENDU) {
            throw new OperationNonAutoriseeException("Résiliation impossible : abonnement " + a.getStatutAbonnement());
        }
        BigDecimal remboursement = calculerRemboursement(a, LocalDateTime.now());
        a.setStatutAbonnement(StatutAbonnement.ANNULE);
        a.setRenouvellementAuto(false);
        return remboursement;
    }

    private BigDecimal calculerRemboursement(Abonnements a, LocalDateTime maintenant) {
        if (a.getStatutPaiement() != StatutPaiement.PAYEE
                || a.getMontantPaye() == null || a.getMontantPaye().signum() == 0) {
            return BigDecimal.ZERO;
        }
        LocalDateTime depart = maintenant.isAfter(a.getDate_debut()) ? maintenant : a.getDate_debut();
        long restant = Duration.between(depart, a.getDate_fin()).getSeconds();
        long total = Duration.between(a.getDate_debut(), a.getDate_fin()).getSeconds();
        if (restant <= 0 || total <= 0) {
            return BigDecimal.ZERO;
        }
        return a.getMontantPaye()
                .multiply(BigDecimal.valueOf(restant))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }



    @Transactional(readOnly = true)
    public boolean estAbonnementValide(String plaque, LocalDateTime maintenant) {
        if (plaque == null || plaque.isBlank()) {
            return false;
        }
        String immat = plaque.trim().toUpperCase();
        return abonnementsRepository
                .findValides(immat, STATUTS_EN_COURS, StatutPaiement.PAYEE, maintenant)
                .stream()
                .anyMatch(a -> dansPlageHoraire(a.getFormule(), maintenant));
    }

    private boolean dansPlageHoraire(FormuleAbonnement formule, LocalDateTime t) {
        FormuleAbonnement f = formule != null ? formule : FormuleAbonnement.COMPLET;
        LocalTime heure = t.toLocalTime();
        boolean jour = !heure.isBefore(DEBUT_JOUR) && heure.isBefore(FIN_JOUR);
        return switch (f) {
            case COMPLET -> true;
            case JOUR -> jour;
            case NUIT -> !jour;
            case WEEKEND -> t.getDayOfWeek() == DayOfWeek.SATURDAY || t.getDayOfWeek() == DayOfWeek.SUNDAY;
        };
    }



    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void expirerAbonnements() {
        expirerAbonnements(LocalDateTime.now());
    }


    @Transactional
    public void expirerAbonnements(LocalDateTime maintenant) {
        for (Abonnements a : abonnementsRepository.findAExpirer(STATUTS_EXPIRABLES, maintenant)) {
            if (a.getStatutAbonnement() == StatutAbonnement.ACTIF && a.isRenouvellementAuto()) {
                try {
                    creerRenouvellement(a, maintenant);
                } catch (OperationNonAutoriseeException e) {
                    log.warn("Renouvellement auto impossible pour l'abonnement {} : {}", a.getAbonnement_id(), e.getMessage());
                }
            }
            a.setStatutAbonnement(StatutAbonnement.EXPIRE);
        }

        for (Abonnements a : abonnementsRepository.findNonPayesAvant(StatutAbonnement.EN_ATTENTE,
                maintenant.minus(DELAI_PAIEMENT))) {
            a.setStatutAbonnement(StatutAbonnement.ANNULE);
        }
    }


    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void envoyerAlertesExpiration() {
        LocalDateTime maintenant = LocalDateTime.now();
        for (Abonnements a : abonnementsRepository.findAAlerter(StatutAbonnement.ACTIF, maintenant,
                maintenant.plus(PREAVIS_ALERTE))) {
            alerteService.alerterUtilisateur(a.getClients(), TypeAlerte.EXPIRATION_ABONNEMENT,
                    "Votre abonnement n° " + a.getAbonnement_id() + " expire le "
                            + a.getDate_fin().format(FORMAT_DATE) + ".",
                    "EXPIRATION_ABONNEMENT:" + a.getAbonnement_id());
            a.setAlerteEnvoyee(true);
        }
    }



    private void verifierAbsenceChevauchement(List<Vehicules> vehicules, LocalDateTime debut, LocalDateTime fin) {
        for (Vehicules v : vehicules) {
            if (abonnementsRepository.existsChevauchement(v.getVehicule_id(), STATUTS_BLOQUANTS, debut, fin)) {
                throw new OperationNonAutoriseeException(
                        "Le véhicule " + v.getImmatriculation() + " a déjà un abonnement sur cette période");
            }
        }
    }

    private void verifierQuota(int nbVehicules, LocalDateTime debut, LocalDateTime fin) {

        List<Places> places = placesRepository.findByTypeForUpdate(TypePlace.ABONNE);
        long capacite = places.stream().filter(p -> p.getStatut() != StatutPlace.HORS_SERVICE).count();
        long dejaPris = abonnementsRepository.compterVehiculesAbonnes(STATUTS_BLOQUANTS, debut, fin);
        if (dejaPris + nbVehicules > capacite) {
            throw new OperationNonAutoriseeException(
                    "Quota de places abonnés atteint (" + dejaPris + "/" + capacite + ")");
        }
    }

    private LocalDateTime calculerFin(LocalDateTime debut, TypeAbonnements duree) {
        return switch (duree) {
            case JOURNALIER -> debut.plusDays(1);
            case HEBDOMADAIRE -> debut.plusWeeks(1);
            case MENSUEL -> debut.plusMonths(1);
            case ANNUEL -> debut.plusYears(1);
        };
    }

    private Abonnements nouvelAbonnement(Clients client, List<Vehicules> vehicules, FormuleAbonnement formule,
                                         TypeAbonnements duree, LocalDateTime debut, LocalDateTime fin,
                                         boolean auto) {
        Abonnements a = new Abonnements();
        a.setClients(client);
        a.setVehicules(new ArrayList<>(vehicules));
        a.setFormule(formule);
        a.setType_abonnements(duree);
        a.setDate_debut(debut);
        a.setDate_fin(fin);
        a.setStatutAbonnement(StatutAbonnement.EN_ATTENTE);
        a.setStatutPaiement(StatutPaiement.EN_ATTENTE);
        a.setRenouvellementAuto(auto);
        a.setAlerteEnvoyee(false);
        return a;
    }

    private Clients trouverClientActif(Long userId) {
        Clients client = clientsRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "avec id inexistant : " + userId));
        if (client.getStatut() != StatutUtilisateur.ACTIF) {
            throw new OperationNonAutoriseeException("Opération refusée : compte non actif");
        }
        return client;
    }

    private List<Vehicules> chargerVehicules(Long userId, List<Long> vehiculeIds) {
        Set<Long> ids = new HashSet<>(vehiculeIds);
        List<Vehicules> vehicules = vehiculesRepository.findAllById(ids);
        if (vehicules.size() != ids.size()) {
            Set<Long> trouves = new HashSet<>();
            vehicules.forEach(v -> trouves.add(v.getVehicule_id()));
            ids.removeAll(trouves);
            throw new ResourceNotFoundException("Vehicule", "avec id inexistant : " + ids);
        }
        for (Vehicules v : vehicules) {
            if (!v.getClients().getClient_id().equals(userId)) {
                throw new OperationNonAutoriseeException(
                        "Le véhicule " + v.getImmatriculation() + " n'appartient pas à ce client");
            }
        }
        return vehicules;
    }

    private Abonnements trouverEntite(Long id) {
        return abonnementsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement", "avec id inexistant : " + id));
    }

    @Transactional(readOnly = true)
    public List<AbonnementsDTO> getAbonnementsParUtilisateur(Long userId) {
        return abonnementsRepository.findByClientId(userId).stream()
                .map(AbonnementsMapper::toDto).toList();
    }
}