package com.projet.parking.service;

import com.projet.parking.dto.PaiementDTO;
import com.projet.parking.dto.RemboursementDTO;
import com.projet.parking.entity.*;
import com.projet.parking.enumeration.*;
import com.projet.parking.exception.OperationNonAutoriseeException;
import com.projet.parking.exception.ResourceNotFoundException;
import com.projet.parking.mapper.PaiementMapper;
import com.projet.parking.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaiementService {

    private static final List<StatutPaiement> EN_ATTENTE = List.of(StatutPaiement.EN_ATTENTE);

    private final PaiementRepository paiementRepository;
    private final EntreeSortiesRepository entreeSortiesRepository;
    private final ReservationRepository reservationRepository;
    private final AbonnementsRepository abonnementsRepository;
    private final FactureRepository factureRepository;
    private final ReservationService reservationService;
    private final AbonnementService abonnementService;
    private final TarifsService tarifsService;

    public PaiementService(PaiementRepository paiementRepository,
                           EntreeSortiesRepository entreeSortiesRepository,
                           ReservationRepository reservationRepository,
                           AbonnementsRepository abonnementsRepository,
                           FactureRepository factureRepository,
                           ReservationService reservationService,
                           AbonnementService abonnementService,
                           TarifsService tarifsService) {
        this.paiementRepository = paiementRepository;
        this.entreeSortiesRepository = entreeSortiesRepository;
        this.reservationRepository = reservationRepository;
        this.abonnementsRepository = abonnementsRepository;
        this.factureRepository = factureRepository;
        this.reservationService = reservationService;
        this.abonnementService = abonnementService;
        this.tarifsService = tarifsService;
    }




    @Transactional
    public PaiementDTO creerPaiement(BigDecimal montant, TypePaiement type, Long cibleId) {
        if (type == null || cibleId == null) {
            throw new OperationNonAutoriseeException("Le type et la cible du paiement sont obligatoires");
        }

        Paiement p = new Paiement();
        switch (type) {
            case SESSION -> {
                EntreeSorties s = entreeSortiesRepository.findById(cibleId)
                        .orElseThrow(() -> new ResourceNotFoundException("Session", "avec id inexistant : " + cibleId));

                if (s.getStatut() == StatutSortie.TERMINE && !factureImpayee(cibleId)) {
                    throw new OperationNonAutoriseeException("Aucun montant à régler pour cette session");
                }
                if (paiementRepository.existsPourSession(cibleId, EN_ATTENTE)) {
                    throw new OperationNonAutoriseeException("Un paiement est déjà en attente pour cette session");
                }
                p.setEntreeSorties(s);
                p.setClients(s.getVehicules().getClients());
            }
            case RESERVATION -> {
                Reservations r = reservationRepository.findById(cibleId)
                        .orElseThrow(() -> new ResourceNotFoundException("Reservation", "avec id inexistant : " + cibleId));
                if (r.getStatut() != StatutReservation.EN_ATTENTE) {
                    throw new OperationNonAutoriseeException("Paiement impossible : réservation " + r.getStatut());
                }
                if (paiementRepository.existsPourReservation(cibleId, EN_ATTENTE)) {
                    throw new OperationNonAutoriseeException("Un paiement est déjà en attente pour cette réservation");
                }
                montant = tarifsService.calculerMontantReservation(r);
                p.setReservations(r);
                p.setClients(r.getClients());
            }
            case ABONNEMENT -> {
                Abonnements a = abonnementsRepository.findById(cibleId)
                        .orElseThrow(() -> new ResourceNotFoundException("Abonnement", "avec id inexistant : " + cibleId));
                if (a.getStatutAbonnement() != StatutAbonnement.EN_ATTENTE) {
                    throw new OperationNonAutoriseeException("Paiement impossible : abonnement " + a.getStatutAbonnement());
                }
                if (paiementRepository.existsPourAbonnement(cibleId, EN_ATTENTE)) {
                    throw new OperationNonAutoriseeException("Un paiement est déjà en attente pour cet abonnement");
                }
                p.setAbonnements(a);
                p.setClients(a.getClients());
            }
        }
        if (montant == null || montant.signum() <= 0) {
            throw new OperationNonAutoriseeException("Le montant calculé doit être strictement positif");
        }
        p.setMontant(montant);
        p.setTypePaiement(type);
        p.setStatut(StatutPaiement.EN_ATTENTE);
        p.setDateCreation(LocalDateTime.now());
        return PaiementMapper.toDto(paiementRepository.save(p));
    }




    @Transactional
    public PaiementDTO validerPaiement(Long id) {
        Paiement p = trouverEntite(id);
        if (p.getStatut() == StatutPaiement.PAYEE) {
            return PaiementMapper.toDto(p);
        }
        if (p.getStatut() != StatutPaiement.EN_ATTENTE) {
            throw new OperationNonAutoriseeException("Validation impossible : paiement " + p.getStatut());
        }
        p.setStatut(StatutPaiement.PAYEE);
        p.setDateValidation(LocalDateTime.now());


        switch (p.getTypePaiement()) {
            case RESERVATION -> reservationService.confirmerReservation(
                    p.getReservations().getReservation_id(), p.getMontant());
            case ABONNEMENT -> abonnementService.confirmerPaiement(
                    p.getAbonnements().getAbonnement_id(), p.getMontant());
            case SESSION -> solderFactureSession(p.getEntreeSorties().getEntreeSortie_id());
        }
        return PaiementMapper.toDto(p);
    }


    @Transactional
    public PaiementDTO marquerEchoue(Long id) {
        Paiement p = trouverEntite(id);
        if (p.getStatut() != StatutPaiement.EN_ATTENTE) {
            throw new OperationNonAutoriseeException("Échec impossible : paiement " + p.getStatut());
        }
        p.setStatut(StatutPaiement.ECHOUEE);
        return PaiementMapper.toDto(p);
    }


    @Transactional
    public PaiementDTO rembourser(Long id) {
        return rembourser(id, null);
    }


    @Transactional
    public PaiementDTO rembourser(Long id, BigDecimal montant) {
        Paiement p = trouverEntite(id);
        if (p.getStatut() != StatutPaiement.PAYEE) {
            throw new OperationNonAutoriseeException("Remboursement impossible : paiement " + p.getStatut());
        }
        BigDecimal aRembourser = montant != null ? montant : p.getMontant();
        if (aRembourser.signum() <= 0 || aRembourser.compareTo(p.getMontant()) > 0) {
            throw new OperationNonAutoriseeException("Montant de remboursement invalide");
        }

        if (p.getTypePaiement() == TypePaiement.RESERVATION
                && p.getReservations().getStatut() != StatutReservation.ANNULEE) {
            throw new OperationNonAutoriseeException("Remboursement impossible : la réservation n'est pas annulée");
        }
        if (p.getTypePaiement() == TypePaiement.ABONNEMENT
                && p.getAbonnements().getStatutAbonnement() != StatutAbonnement.ANNULE) {
            throw new OperationNonAutoriseeException("Remboursement impossible : l'abonnement n'est pas résilié");
        }
        p.setMontantRembourse(aRembourser);
        p.setStatut(StatutPaiement.REMBOURSEE);
        return PaiementMapper.toDto(p);
    }







    @Transactional
    public RemboursementDTO annulerReservationEtRembourser(Long reservationId) {
        BigDecimal montant = reservationService.annulerReservation(reservationId);
        abandonnerPaiementsEnAttente(
                paiementRepository.findByReservationIdAndStatut(reservationId, StatutPaiement.EN_ATTENTE));
        return rembourserSiPaye(
                paiementRepository.findByReservationIdAndStatut(reservationId, StatutPaiement.PAYEE), montant);
    }


    @Transactional
    public RemboursementDTO resilierAbonnementEtRembourser(Long abonnementId) {
        BigDecimal montant = abonnementService.resilier(abonnementId);
        abandonnerPaiementsEnAttente(
                paiementRepository.findByAbonnementIdAndStatut(abonnementId, StatutPaiement.EN_ATTENTE));
        return rembourserSiPaye(
                paiementRepository.findByAbonnementIdAndStatut(abonnementId, StatutPaiement.PAYEE), montant);
    }


    private void abandonnerPaiementsEnAttente(List<Paiement> enAttente) {
        enAttente.forEach(p -> p.setStatut(StatutPaiement.ANNULLEE));
    }


    private RemboursementDTO rembourserSiPaye(List<Paiement> payes, BigDecimal montant) {
        if (payes.isEmpty() || montant == null || montant.signum() <= 0) {
            return new RemboursementDTO(BigDecimal.ZERO, null);
        }
        Paiement paiement = payes.get(0);
        rembourser(paiement.getPaiement_id(), montant);
        return new RemboursementDTO(montant, paiement.getPaiement_id());
    }



    @Transactional(readOnly = true)
    public List<PaiementDTO> getPaiementsParUtilisateur(Long userId) {
        return paiementRepository.findByClientId(userId).stream().map(PaiementMapper::toDto).toList();
    }




    @Transactional(readOnly = true)
    public BigDecimal totalValideSession(Long sessionId) {
        BigDecimal total = paiementRepository.totalParSession(sessionId, StatutPaiement.PAYEE);
        return total != null ? total : BigDecimal.ZERO;
    }


    private void solderFactureSession(Long sessionId) {
        factureRepository.findBySessionId(sessionId)
                .filter(f -> f.getStatutPaiement() == StatutPaiement.EN_ATTENTE)
                .ifPresent(f -> {
                    if (totalValideSession(sessionId).compareTo(f.getMontantTotal()) >= 0) {
                        f.setStatutPaiement(StatutPaiement.PAYEE);
                    }
                });
    }

    private boolean factureImpayee(Long sessionId) {
        return factureRepository.findBySessionId(sessionId)
                .filter(f -> f.getStatutPaiement() == StatutPaiement.EN_ATTENTE).isPresent();
    }

    private Paiement trouverEntite(Long id) {
        return paiementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement", "avec id inexistant : " + id));
    }

    @Transactional(readOnly = true)
    public List<PaiementDTO> getPaiementsParStatut(StatutPaiement statut) {
        return paiementRepository.findByStatutOrderByDateCreationDesc(statut).stream()
                .map(PaiementMapper::toDto).toList();
    }
}
