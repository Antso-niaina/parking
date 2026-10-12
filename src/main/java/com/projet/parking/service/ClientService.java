package com.projet.parking.service;

import com.projet.parking.dto.ClientsDTO;
import com.projet.parking.entity.Clients;
import com.projet.parking.enumeration.StatutAbonnement;
import com.projet.parking.enumeration.StatutPaiement;           // ajouté
import com.projet.parking.enumeration.StatutReservation;
import com.projet.parking.enumeration.StatutUtilisateur;
import com.projet.parking.exception.OperationNonAutoriseeException;
import com.projet.parking.exception.ResourceNotFoundException;
import com.projet.parking.mapper.ClientsMapper;
import com.projet.parking.repository.AbonnementsRepository;
import com.projet.parking.repository.ClientsRepository;
import com.projet.parking.repository.EntreeSortiesRepository;
import com.projet.parking.repository.FactureRepository;         // ajouté
import com.projet.parking.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClientService {

    private final ClientsRepository clientsRepository;
    private final ReservationRepository reservationRepository;
    private final AbonnementsRepository abonnementsRepository;
    private final EntreeSortiesRepository entreeSortiesRepository;
    private final FactureRepository factureRepository;          // ajouté

    public ClientService(ClientsRepository clientsRepository,
                         ReservationRepository reservationRepository,
                         AbonnementsRepository abonnementsRepository,
                         EntreeSortiesRepository entreeSortiesRepository,
                         FactureRepository factureRepository) { // ajouté
        this.clientsRepository = clientsRepository;
        this.reservationRepository = reservationRepository;
        this.abonnementsRepository = abonnementsRepository;
        this.entreeSortiesRepository = entreeSortiesRepository;
        this.factureRepository = factureRepository;             // ajouté
    }

    @Transactional(readOnly = true)
    public ClientsDTO recuperer(Long id) {
        return ClientsMapper.toDto(trouverEntite(id));
    }

    @Transactional
    public ClientsDTO modifier(Long id, ClientsDTO dto) {
        Clients existing = trouverEntite(id);
        ClientsMapper.updateEntity(existing, dto);
        return ClientsMapper.toDto(existing);
    }

    @Transactional
    public ClientsDTO suspendreUtilisateur(Long id) {
        Clients client = trouverEntite(id);
        if (client.getStatut() == StatutUtilisateur.SUSPENDU) {
            throw new OperationNonAutoriseeException("Le client est déjà suspendu");
        }
        client.setStatut(StatutUtilisateur.SUSPENDU);
        return ClientsMapper.toDto(client);
    }

    @Transactional
    public ClientsDTO reactiverUtilisateur(Long id) {
        Clients client = trouverEntite(id);
        if (client.getStatut() == StatutUtilisateur.ACTIF) {
            throw new OperationNonAutoriseeException("Le client est déjà actif");
        }
        client.setStatut(StatutUtilisateur.ACTIF);
        return ClientsMapper.toDto(client);
    }

    @Transactional
    public void supprimerUtilisateur(Long id) {
        Clients client = trouverEntite(id);

        if (reservationRepository.existsActiveByClientId(
                id, List.of(StatutReservation.EN_ATTENTE,
                        StatutReservation.CONFIRMEE,
                        StatutReservation.EN_COURS))) {
            throw new OperationNonAutoriseeException("Suppression refusée : le client a une réservation active");
        }
        if (abonnementsRepository.existsActiveByClientId(
                id, List.of(StatutAbonnement.EN_ATTENTE,
                        StatutAbonnement.ACTIF,
                        StatutAbonnement.SUSPENDU),
                LocalDateTime.now())) {
            throw new OperationNonAutoriseeException("Suppression refusée : le client a un abonnement actif");
        }
        if (entreeSortiesRepository.existsActiveByClientId(id)) {
            throw new OperationNonAutoriseeException("Suppression refusée : le client a une session active");
        }

        client.setStatut(StatutUtilisateur.SUPPRIME);
    }

    @Transactional(readOnly = true)
    public void verifierImpayes(Long id) {
        trouverEntite(id);
        if (factureRepository.existsImpayeByClientId(id, StatutPaiement.EN_ATTENTE)) {
            throw new OperationNonAutoriseeException(
                    "Réservation refusée : le client a des factures impayées");
        }
    }

    private Clients trouverEntite(Long id) {
        return clientsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "avec id inexistant : " + id));
    }
}