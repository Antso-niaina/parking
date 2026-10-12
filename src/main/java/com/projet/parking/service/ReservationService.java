package com.projet.parking.service;

import com.projet.parking.dto.ReservationsDTO;
import com.projet.parking.entity.*;
import com.projet.parking.enumeration.*;
import com.projet.parking.exception.OperationNonAutoriseeException;
import com.projet.parking.exception.ResourceNotFoundException;
import com.projet.parking.mapper.ReservationsMapper;
import com.projet.parking.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReservationService {

    private static final List<StatutReservation> STATUTS_BLOQUANTS =
            List.of(StatutReservation.EN_ATTENTE, StatutReservation.CONFIRMEE, StatutReservation.EN_COURS);

    private static final Duration DUREE_MIN = Duration.ofMinutes(30);
    private static final Duration DUREE_MAX = Duration.ofHours(24);
    private static final Duration DELAI_GRACE = Duration.ofMinutes(15);
    private static final Duration TOLERANCE_ENTREE_ANTICIPEE = Duration.ofMinutes(15);
    private static final BigDecimal PENALITE_NO_SHOW = new BigDecimal("5.00");

    private final ReservationRepository reservationRepository;
    private final ClientsRepository clientsRepository;
    private final VehiculesRepository vehiculesRepository;
    private final PlacesRepository placesRepository;
    private final ClientService clientService;
    private final PlacesService placesService;
    private final PlacePrioritaireService placePrioritaireService;   // NOUVEAU

    public ReservationService(ReservationRepository reservationRepository,
                              ClientsRepository clientsRepository,
                              VehiculesRepository vehiculesRepository,
                              PlacesRepository placesRepository,
                              ClientService clientService,
                              PlacesService placesService,
                              PlacePrioritaireService placePrioritaireService) {   // NOUVEAU
        this.reservationRepository = reservationRepository;
        this.clientsRepository = clientsRepository;
        this.vehiculesRepository = vehiculesRepository;
        this.placesRepository = placesRepository;
        this.clientService = clientService;
        this.placesService = placesService;
        this.placePrioritaireService = placePrioritaireService;                   // NOUVEAU
    }

    @Transactional
    public ReservationsDTO creerReservation(Long userId, Long vehiculeId, Long placeId,
                                            LocalDateTime debut, LocalDateTime fin) {
        LocalDateTime maintenant = LocalDateTime.now();
        validerCreneau(debut, fin, maintenant);

        Clients client = clientsRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "avec id inexistant : " + userId));
        if (client.getStatut() != StatutUtilisateur.ACTIF) {
            throw new OperationNonAutoriseeException("Réservation refusée : compte non actif");
        }
        clientService.verifierImpayes(userId);

        Vehicules vehicule = vehiculesRepository.findById(vehiculeId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicule", "avec id inexistant : " + vehiculeId));
        if (!vehicule.getClients().getClient_id().equals(userId)) {
            throw new OperationNonAutoriseeException("Ce véhicule n'appartient pas à ce client");
        }

        Places place = trouverPlace(placeId);
        verifierPlaceReservable(place, vehicule);
        verifierDisponibilite(place.getPlace_id(), vehiculeId, -1L, debut, fin);

        Reservations r = new Reservations();
        r.setDate_heure_arrive(debut);
        r.setDate_heure_depart(fin);
        r.setDate_creation(maintenant);
        r.setStatut(StatutReservation.EN_ATTENTE);
        r.setClients(client);
        r.setVehicules(vehicule);
        r.setPlaces(place);
        return ReservationsMapper.toDto(reservationRepository.save(r));
    }

    @Transactional
    public ReservationsDTO modifierReservation(Long id, LocalDateTime nouveauDebut,
                                               LocalDateTime nouvelleFin, Long nouvellePlaceId) {
        Reservations r = trouverEntite(id);
        if (r.getStatut() != StatutReservation.EN_ATTENTE && r.getStatut() != StatutReservation.CONFIRMEE) {
            throw new OperationNonAutoriseeException("Modification impossible : réservation " + r.getStatut());
        }
        LocalDateTime debut = nouveauDebut != null ? nouveauDebut : r.getDate_heure_arrive();
        LocalDateTime fin = nouvelleFin != null ? nouvelleFin : r.getDate_heure_depart();
        validerCreneau(debut, fin, LocalDateTime.now());

        Places place = nouvellePlaceId != null ? trouverPlace(nouvellePlaceId) : r.getPlaces();
        verifierPlaceReservable(place, r.getVehicules());
        verifierDisponibilite(place.getPlace_id(), r.getVehicules().getVehicule_id(), id, debut, fin);

        r.setDate_heure_arrive(debut);
        r.setDate_heure_depart(fin);
        r.setPlaces(place);
        return ReservationsMapper.toDto(r);
    }


    @Transactional
    public BigDecimal annulerReservation(Long id) {
        Reservations r = trouverEntite(id);
        if (r.getStatut() != StatutReservation.EN_ATTENTE && r.getStatut() != StatutReservation.CONFIRMEE) {
            throw new OperationNonAutoriseeException("Annulation impossible : réservation " + r.getStatut());
        }
        BigDecimal remboursement = calculerRemboursement(r, LocalDateTime.now());
        r.setStatut(StatutReservation.ANNULEE);
        libererPlaceSiReservee(r.getPlaces());
        return remboursement;
    }

    private BigDecimal calculerRemboursement(Reservations r, LocalDateTime maintenant) {
        if (r.getMontantPaye() == null || r.getMontantPaye().signum() == 0) {
            return BigDecimal.ZERO;
        }
        Duration delai = Duration.between(maintenant, r.getDate_heure_arrive());
        BigDecimal taux;
        if (delai.compareTo(Duration.ofHours(24)) >= 0) taux = BigDecimal.ONE;
        else if (delai.compareTo(Duration.ofHours(2)) >= 0) taux = new BigDecimal("0.50");
        else taux = BigDecimal.ZERO;
        return r.getMontantPaye().multiply(taux).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public ReservationsDTO confirmerReservation(Long id, BigDecimal montantPaye) {
        Reservations r = trouverEntite(id);
        if (r.getStatut() != StatutReservation.EN_ATTENTE) {
            throw new OperationNonAutoriseeException("Confirmation impossible : réservation " + r.getStatut());
        }
        if (montantPaye == null || montantPaye.signum() <= 0) {
            throw new OperationNonAutoriseeException("Le montant payé est obligatoire");
        }
        if (!r.getDate_heure_arrive().isAfter(LocalDateTime.now().minus(DELAI_GRACE))) {
            throw new OperationNonAutoriseeException("Confirmation impossible : le créneau est déjà passé");
        }
        r.setMontantPaye(montantPaye);
        r.setStatut(StatutReservation.CONFIRMEE);
        return ReservationsMapper.toDto(r);
    }

    @Transactional(readOnly = true)
    public Optional<Reservations> verifierReservationValide(String immatriculation, LocalDateTime maintenant) {
        if (immatriculation == null || immatriculation.isBlank()) {
            return Optional.empty();
        }
        return reservationRepository.findAccesValides(
                        immatriculation.trim().toUpperCase(),
                        StatutReservation.CONFIRMEE,
                        maintenant.plus(TOLERANCE_ENTREE_ANTICIPEE),
                        maintenant)
                .stream().findFirst();
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expirerReservationsNoShow() {
        LocalDateTime limite = LocalDateTime.now().minus(DELAI_GRACE);
        for (Reservations r : reservationRepository.findNoShows(StatutReservation.CONFIRMEE, limite)) {
            r.setStatut(StatutReservation.NO_SHOW);
            r.setMontantPenalite(PENALITE_NO_SHOW);
            libererPlaceSiReservee(r.getPlaces());
        }
    }

    @Transactional(readOnly = true)
    public Optional<Places> proposerPlaceEquivalente(Reservations reservation) {
        Places actuelle = reservation.getPlaces();
        return placesRepository.trouverDisponibles(
                        List.of(actuelle.getType_place()), StatutPlace.HORS_SERVICE, STATUTS_BLOQUANTS,
                        reservation.getDate_heure_arrive(), reservation.getDate_heure_depart())
                .stream()
                .filter(p -> !p.getPlace_id().equals(actuelle.getPlace_id()))
                .filter(p -> placesService.estCompatible(p, reservation.getVehicules()))
                .findFirst();
    }

    private void validerCreneau(LocalDateTime debut, LocalDateTime fin, LocalDateTime maintenant) {
        if (debut == null || fin == null) {
            throw new OperationNonAutoriseeException("Début et fin sont obligatoires");
        }
        if (!fin.isAfter(debut)) {
            throw new OperationNonAutoriseeException("La fin doit être après le début");
        }
        if (debut.isBefore(maintenant)) {
            throw new OperationNonAutoriseeException("Le début ne peut pas être dans le passé");
        }
        Duration duree = Duration.between(debut, fin);
        if (duree.compareTo(DUREE_MIN) < 0) {
            throw new OperationNonAutoriseeException("Durée minimale : " + DUREE_MIN.toMinutes() + " minutes");
        }
        if (duree.compareTo(DUREE_MAX) > 0) {
            throw new OperationNonAutoriseeException("Durée maximale : " + DUREE_MAX.toHours() + " heures");
        }
    }

    private void verifierPlaceReservable(Places place, Vehicules vehicule) {
        if (place.getStatut() == StatutPlace.HORS_SERVICE) {
            throw new OperationNonAutoriseeException("Cette place est hors service");
        }
        if (!placesService.estCompatible(place, vehicule)) {
            throw new OperationNonAutoriseeException("Place incompatible avec le type de véhicule");
        }
        // NOUVEAU : une place prioritaire n'est réservable que par un ayant droit vérifié
        if (place.getType_place() == TypePlace.PRM) {
            placePrioritaireService.verifierEligibilite(vehicule.getClients(), vehicule);
        }
    }

    private void verifierDisponibilite(Long placeId, Long vehiculeId, Long excludeId,
                                       LocalDateTime debut, LocalDateTime fin) {
        if (reservationRepository.existsChevauchementPlace(placeId, excludeId, STATUTS_BLOQUANTS, debut, fin)) {
            throw new OperationNonAutoriseeException("Place déjà réservée sur ce créneau");
        }
        if (reservationRepository.existsChevauchementVehicule(vehiculeId, excludeId, STATUTS_BLOQUANTS, debut, fin)) {
            throw new OperationNonAutoriseeException("Ce véhicule a déjà une réservation sur ce créneau");
        }
    }

    private void libererPlaceSiReservee(Places place) {
        if (place.getStatut() == StatutPlace.RESERVEE) {
            place.setStatut(StatutPlace.LIBRE);
        }
    }

    private Places trouverPlace(Long id) {
        return placesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Place", "avec id inexistant : " + id));
    }

    private Reservations trouverEntite(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "avec id inexistant : " + id));
    }

    @Transactional(readOnly = true)
    public List<ReservationsDTO> getReservationsParUtilisateur(Long userId) {
        return reservationRepository.findByClientId(userId).stream()
                .map(ReservationsMapper::toDto).toList();
    }
}