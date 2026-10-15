package com.projet.parking.service;

import com.projet.parking.dto.FactureDTO;
import com.projet.parking.dto.RecuDTO;
import com.projet.parking.entity.Paiement;
import com.projet.parking.enumeration.StatutPaiement;
import com.projet.parking.enumeration.TypePaiement;
import com.projet.parking.exception.OperationNonAutoriseeException;
import com.projet.parking.exception.ResourceNotFoundException;
import com.projet.parking.mapper.FactureMapper;
import com.projet.parking.repository.ClientsRepository;
import com.projet.parking.repository.FactureRepository;
import com.projet.parking.repository.PaiementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class FactureService {

    private final FactureRepository factureRepository;
    private final PaiementRepository paiementRepository;
    private final ClientsRepository clientsRepository;

    public FactureService(FactureRepository factureRepository,
                          PaiementRepository paiementRepository,
                          ClientsRepository clientsRepository) {
        this.factureRepository = factureRepository;
        this.paiementRepository = paiementRepository;
        this.clientsRepository = clientsRepository;
    }

    @Transactional(readOnly = true)
    public void verifierImpayes(Long clientId) {
        if (factureRepository.existsImpayeByClientId(clientId, StatutPaiement.EN_ATTENTE)) {
            throw new OperationNonAutoriseeException(
                    "Réservation refusée : le client a des factures impayées");
        }
    }



    @Transactional(readOnly = true)
    public List<FactureDTO> getImpayes(Long userId) {
        clientsRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "avec id inexistant : " + userId));
        return factureRepository.findByClientIdAndStatut(userId, StatutPaiement.EN_ATTENTE)
                .stream().map(FactureMapper::toDto).toList();
    }


    @Transactional(readOnly = true)
    public FactureDTO genererFacture(Long paiementId) {
        Paiement p = trouverPaiement(paiementId);
        if (p.getTypePaiement() != TypePaiement.SESSION) {
            throw new OperationNonAutoriseeException("Pas de facture pour ce type de paiement : utiliser genererRecu");
        }
        return factureRepository.findBySessionId(p.getEntreeSorties().getEntreeSortie_id())
                .map(FactureMapper::toDto)
                .orElseThrow(() -> new OperationNonAutoriseeException("La facture sera disponible à la sortie du véhicule"));
    }


    @Transactional(readOnly = true)
    public RecuDTO genererRecu(Long paiementId) {
        Paiement p = trouverPaiement(paiementId);
        if (p.getStatut() != StatutPaiement.PAYEE && p.getStatut() != StatutPaiement.REMBOURSEE) {
            throw new OperationNonAutoriseeException("Aucun reçu : paiement " + p.getStatut());
        }
        String objet = switch (p.getTypePaiement()) {
            case SESSION -> "Stationnement (session n° " + p.getEntreeSorties().getEntreeSortie_id() + ")";
            case RESERVATION -> "Réservation n° " + p.getReservations().getReservation_id();
            case ABONNEMENT -> "Abonnement n° " + p.getAbonnements().getAbonnement_id();
        };
        String client = p.getClients() != null
                ? p.getClients().getPrenom() + " " + p.getClients().getNom()
                : "Client occasionnel";
        String numero = "REC-" + p.getDateValidation().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%06d", p.getPaiement_id());
        return new RecuDTO(numero, p.getDateValidation(), client, objet,
                p.getMontant(), p.getMontantRembourse(), p.getStatut());
    }

    private Paiement trouverPaiement(Long id) {
        return paiementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement", "avec id inexistant : " + id));
    }
}