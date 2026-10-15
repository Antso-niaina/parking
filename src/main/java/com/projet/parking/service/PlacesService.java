package com.projet.parking.service;

import com.projet.parking.dto.PlacesDTO;
import com.projet.parking.entity.Places;
import com.projet.parking.entity.Vehicules;
import com.projet.parking.entity.Zones;
import com.projet.parking.enumeration.StatutPlace;
import com.projet.parking.enumeration.StatutReservation;
import com.projet.parking.enumeration.TypePlace;
import com.projet.parking.enumeration.TypeVehicule;
import com.projet.parking.exception.OperationNonAutoriseeException;
import com.projet.parking.exception.ResourceNotFoundException;
import com.projet.parking.mapper.PlacesMapper;
import com.projet.parking.repository.EntreeSortiesRepository;
import com.projet.parking.repository.PlacesRepository;
import com.projet.parking.repository.ReservationRepository;
import com.projet.parking.repository.ZonesRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.projet.parking.enumeration.StatutPlace.*;

@Service
public class PlacesService {

    private static final List<StatutReservation> RESERVATIONS_ACTIVES =
            List.of(StatutReservation.EN_ATTENTE, StatutReservation.CONFIRMEE, StatutReservation.EN_COURS);

    private static final Map<StatutPlace, Set<StatutPlace>> TRANSITIONS = new EnumMap<>(StatutPlace.class);
    private static final Map<TypeVehicule, List<TypePlace>> COMPATIBILITE = new EnumMap<>(TypeVehicule.class);

    static {
        TRANSITIONS.put(LIBRE, EnumSet.of(RESERVEE, OCCUPEE, HORS_SERVICE));
        TRANSITIONS.put(RESERVEE, EnumSet.of(LIBRE, OCCUPEE));
        TRANSITIONS.put(OCCUPEE, EnumSet.of(LIBRE));
        TRANSITIONS.put(HORS_SERVICE, EnumSet.of(LIBRE));

        COMPATIBILITE.put(TypeVehicule.VOITURE, List.of(TypePlace.STANDARD));
        COMPATIBILITE.put(TypeVehicule.MOTO, List.of(TypePlace.STANDARD));
        COMPATIBILITE.put(TypeVehicule.UTILITAIRE, List.of(TypePlace.STANDARD));
        COMPATIBILITE.put(TypeVehicule.PMR, List.of(TypePlace.PRM, TypePlace.STANDARD));
    }

    private final PlacesRepository placesRepository;
    private final ReservationRepository reservationRepository;
    private final EntreeSortiesRepository entreeSortiesRepository;
    private final ZonesRepository zonesRepository;

    public PlacesService(PlacesRepository placesRepository,
                         ReservationRepository reservationRepository,
                         EntreeSortiesRepository entreeSortiesRepository,
                         ZonesRepository zonesRepository) {
        this.placesRepository = placesRepository;
        this.reservationRepository = reservationRepository;
        this.entreeSortiesRepository = entreeSortiesRepository;
        this.zonesRepository = zonesRepository;
    }


    @Transactional
    public Places creerPlace(String numero, int niveau, TypePlace type, Long zoneId) {
        if (numero == null || numero.isBlank()) {
            throw new OperationNonAutoriseeException("Le numéro de place est obligatoire");
        }
        if (type == null) {
            throw new OperationNonAutoriseeException("Le type de place est obligatoire");
        }
        if (zoneId == null) {
            throw new OperationNonAutoriseeException("La zone est obligatoire");
        }
        String numeroNormalise = numero.trim().toUpperCase();
        if (placesRepository.existeParNumero(numeroNormalise)) {
            throw new OperationNonAutoriseeException("Une place avec ce numéro existe déjà");
        }

        Zones zone = verrouillerZone(zoneId);
        verifierCapaciteZone(zone);

        Places place = new Places();
        place.setNumero(numeroNormalise);
        place.setNiveau(niveau);
        place.setType_place(type);
        place.setStatut(LIBRE);
        place.setZones(zone);
        return placesRepository.save(place);
    }


    @Transactional
    public Places affecterZone(Long placeId, Long zoneId) {
        Places place = trouverEntite(placeId);
        Zones zone = verrouillerZone(zoneId);
        if (place.getZones() != null && place.getZones().getZone_id().equals(zoneId)) {
            return place;
        }
        verifierCapaciteZone(zone);
        place.setZones(zone);
        return place;
    }

    @Transactional
    public Places changerStatut(Long placeId, StatutPlace nouveauStatut) {
        Places place = trouverEntite(placeId);
        StatutPlace actuel = place.getStatut();

        if (actuel == nouveauStatut) {
            throw new OperationNonAutoriseeException("La place est déjà au statut " + actuel);
        }
        if (!TRANSITIONS.getOrDefault(actuel, Set.of()).contains(nouveauStatut)) {
            throw new OperationNonAutoriseeException("Transition interdite : " + actuel + " → " + nouveauStatut);
        }
        if (nouveauStatut == HORS_SERVICE) {
            verifierMaintenancePossible(place);
        }

        place.setStatut(nouveauStatut);
        return place;
    }

    @Transactional
    public Places mettreEnMaintenance(Long placeId) {
        Places place = trouverEntite(placeId);
        if (place.getStatut() == HORS_SERVICE) {
            throw new OperationNonAutoriseeException("La place est déjà hors service");
        }
        verifierMaintenancePossible(place);
        place.setStatut(HORS_SERVICE);
        return place;
    }

    @Transactional(readOnly = true)
    public Places trouverPlaceDisponible(TypeVehicule typeVehicule, LocalDateTime debut, LocalDateTime fin) {
        if (typeVehicule == null || debut == null || fin == null) {
            throw new OperationNonAutoriseeException("Type de véhicule, début et fin sont obligatoires");
        }
        if (!fin.isAfter(debut)) {
            throw new OperationNonAutoriseeException("La fin doit être après le début");
        }

        List<TypePlace> typesCompatibles = COMPATIBILITE.getOrDefault(typeVehicule, List.of());
        return placesRepository
                .trouverDisponibles(typesCompatibles, HORS_SERVICE, RESERVATIONS_ACTIVES, debut, fin)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Place", "disponible pour ce créneau"));
    }

    public boolean estCompatible(Places place, Vehicules vehicule) {
        return COMPATIBILITE.getOrDefault(vehicule.getType_vehicule(), List.of())
                .contains(place.getType_place());
    }

    @Transactional(readOnly = true)
    public double getTauxOccupation() {
        long exploitables = placesRepository.count() - placesRepository.compterParStatut(HORS_SERVICE);
        if (exploitables == 0) {
            return 0.0;
        }
        long occupees = placesRepository.compterParStatut(OCCUPEE);
        return Math.round(occupees * 10000.0 / exploitables) / 100.0;
    }

    @Transactional(readOnly = true)
    public Map<TypePlace, Long> getPlacesDisponiblesParType() {
        Map<TypePlace, Long> resultat = new EnumMap<>(TypePlace.class);
        for (TypePlace t : TypePlace.values()) {
            resultat.put(t, 0L);
        }
        for (Object[] ligne : placesRepository.compterParTypeEtStatut(LIBRE)) {
            resultat.put((TypePlace) ligne[0], (Long) ligne[1]);
        }
        return resultat;
    }


    private Zones verrouillerZone(Long zoneId) {
        return zonesRepository.findByIdForUpdate(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone", "avec id inexistant : " + zoneId));
    }


    private void verifierCapaciteZone(Zones zone) {
        long existantes = placesRepository.compterParZone(zone.getZone_id());
        if (existantes >= zone.getCapaciteTotal()) {
            throw new OperationNonAutoriseeException("Zone « " + zone.getNom() + " » pleine : capacité de "
                    + zone.getCapaciteTotal() + " place(s) atteinte");
        }
    }

    private void verifierMaintenancePossible(Places place) {
        if (place.getStatut() == OCCUPEE || place.getStatut() == RESERVEE) {
            throw new OperationNonAutoriseeException("Mise hors service refusée : la place est " + place.getStatut());
        }
        if (entreeSortiesRepository.existsPresentByPlaceId(place.getPlace_id())) {
            throw new OperationNonAutoriseeException("Mise hors service refusée : un véhicule est sur la place");
        }
        if (reservationRepository.existsReservationEnCoursOuAVenir(
                place.getPlace_id(), RESERVATIONS_ACTIVES, LocalDateTime.now())) {
            throw new OperationNonAutoriseeException(
                    "Mise hors service refusée : une réservation est en cours ou à venir sur cette place");
        }
    }

    private Places trouverEntite(Long id) {
        return placesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Place", "avec id inexistant : " + id));
    }
    @Transactional
    public List<PlacesDTO> listerPlaces() {
        LocalDateTime maintenant = LocalDateTime.now();
        List<Places> places = placesRepository.findAll(Sort.by("numero"));
        for (Places place : places) {
            boolean reservee = reservationRepository.existsReservationEnCoursOuAVenir(
                    place.getPlace_id(), RESERVATIONS_ACTIVES, maintenant);
            if (reservee && place.getStatut() == LIBRE) {
                place.setStatut(RESERVEE);
            } else if (!reservee && place.getStatut() == RESERVEE) {
                place.setStatut(LIBRE);
            }
        }
        return places.stream().map(PlacesMapper::toDto).toList();
    }
}
