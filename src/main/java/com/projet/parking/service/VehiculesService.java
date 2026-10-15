package com.projet.parking.service;

import com.projet.parking.dto.VehiculesDTO;
import com.projet.parking.entity.Clients;
import com.projet.parking.entity.Vehicules;
import com.projet.parking.enumeration.StatutAbonnement;
import com.projet.parking.enumeration.StatutReservation;
import com.projet.parking.enumeration.StatutUtilisateur;
import com.projet.parking.exception.OperationNonAutoriseeException;
import com.projet.parking.exception.ResourceNotFoundException;
import com.projet.parking.mapper.VehiculesMapper;
import com.projet.parking.repository.AbonnementsRepository;
import com.projet.parking.repository.ClientsRepository;
import com.projet.parking.repository.EntreeSortiesRepository;
import com.projet.parking.repository.ReservationRepository;
import com.projet.parking.repository.VehiculesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VehiculesService {

    private static final List<StatutReservation> RESERVATIONS_ACTIVES =
            List.of(StatutReservation.EN_ATTENTE, StatutReservation.CONFIRMEE, StatutReservation.EN_COURS);

    private static final List<StatutAbonnement> ABONNEMENTS_ACTIFS =
            List.of(StatutAbonnement.EN_ATTENTE, StatutAbonnement.ACTIF, StatutAbonnement.SUSPENDU);

    private final VehiculesRepository vehiculesRepository;
    private final ClientsRepository clientsRepository;
    private final EntreeSortiesRepository entreeSortiesRepository;
    private final ReservationRepository reservationRepository;
    private final AbonnementsRepository abonnementsRepository;

    public VehiculesService(VehiculesRepository vehiculesRepository,
                           ClientsRepository clientsRepository,
                           EntreeSortiesRepository entreeSortiesRepository,
                           ReservationRepository reservationRepository,
                           AbonnementsRepository abonnementsRepository) {
        this.vehiculesRepository = vehiculesRepository;
        this.clientsRepository = clientsRepository;
        this.entreeSortiesRepository = entreeSortiesRepository;
        this.reservationRepository = reservationRepository;
        this.abonnementsRepository = abonnementsRepository;
    }

    @Transactional
    public VehiculesDTO enregistrerVehicule(VehiculesDTO dto) {
        if (dto.getType_vehicule() == null) {
            throw new OperationNonAutoriseeException("Le type de véhicule est obligatoire");
        }
        String immat = normaliser(dto.getImmatriculation());
        if (vehiculesRepository.existeParImmatriculation(immat)) {
            throw new OperationNonAutoriseeException("Un véhicule avec cette immatriculation existe déjà");
        }

        Clients proprietaire = clientsRepository.findById(dto.getClient_id())
                .orElseThrow(() -> new ResourceNotFoundException("Client", "avec id inexistant : " + dto.getClient_id()));
        if (proprietaire.getStatut() != StatutUtilisateur.ACTIF) {
            throw new OperationNonAutoriseeException("Le propriétaire n'a pas un compte actif");
        }

        dto.setImmatriculation(immat);
        Vehicules saved = vehiculesRepository.save(VehiculesMapper.toEntity(dto, proprietaire));
        return VehiculesMapper.toDto(saved);
    }

    @Transactional
    public VehiculesDTO modifierVehicule(Long id, VehiculesDTO dto) {
        Vehicules existing = trouverEntite(id);
        if (dto.getType_vehicule() == null) {
            throw new OperationNonAutoriseeException("Le type de véhicule est obligatoire");
        }
        String immat = normaliser(dto.getImmatriculation());
        if (vehiculesRepository.existsByImmatriculationAndIdNot(immat, id)) {
            throw new OperationNonAutoriseeException("Un autre véhicule utilise déjà cette immatriculation");
        }

        dto.setImmatriculation(immat);
        VehiculesMapper.updateEntity(existing, dto);
        return VehiculesMapper.toDto(existing);
    }

    @Transactional
    public void supprimerVehicule(Long id) {
        Vehicules vehicule = trouverEntite(id);

        if (entreeSortiesRepository.existsPresentByVehiculeId(id)) {
            throw new OperationNonAutoriseeException("Suppression refusée : le véhicule est dans le parking");
        }

        Long proprietaireId = vehicule.getClients().getClient_id();
        if (reservationRepository.existsActiveByClientId(proprietaireId, RESERVATIONS_ACTIVES)) {
            throw new OperationNonAutoriseeException("Suppression refusée : une réservation active existe");
        }
        if (abonnementsRepository.existsActiveByClientId(proprietaireId, ABONNEMENTS_ACTIFS, LocalDateTime.now())) {
            throw new OperationNonAutoriseeException("Suppression refusée : un abonnement actif existe");
        }

        vehiculesRepository.delete(vehicule);
    }

    @Transactional(readOnly = true)
    public boolean estPresentDansParking(String immatriculation) {
        return entreeSortiesRepository.existsPresentByImmatriculation(normaliser(immatriculation));
    }

    @Transactional(readOnly = true)
    public List<VehiculesDTO> getVehiculesParUtilisateur(Long userId) {
        if (!clientsRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Client", "avec id inexistant : " + userId);
        }
        return vehiculesRepository.findByClientId(userId).stream()
                .map(VehiculesMapper::toDto)
                .toList();
    }

    private Vehicules trouverEntite(Long id) {
        return vehiculesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicule", "avec id inexistant : " + id));
    }

    private String normaliser(String immatriculation) {
        if (immatriculation == null || immatriculation.isBlank()) {
            throw new OperationNonAutoriseeException("L'immatriculation est obligatoire");
        }
        return immatriculation.trim().toUpperCase();
    }
}
