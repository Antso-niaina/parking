package com.projet.parking.security;

import com.projet.parking.entity.Clients;
import com.projet.parking.enumeration.TypePaiement;
import com.projet.parking.repository.AbonnementsRepository;
import com.projet.parking.repository.AlerteRepository;
import com.projet.parking.repository.PaiementRepository;
import com.projet.parking.repository.ReservationRepository;
import com.projet.parking.repository.VehiculesRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("securite")
@Transactional(readOnly = true)
public class SecuriteService {

    private final ReservationRepository reservationRepository;
    private final AbonnementsRepository abonnementsRepository;
    private final VehiculesRepository vehiculesRepository;
    private final PaiementRepository paiementRepository;
    private final AlerteRepository alerteRepository;

    public SecuriteService(ReservationRepository reservationRepository,
                           AbonnementsRepository abonnementsRepository,
                           VehiculesRepository vehiculesRepository,
                           PaiementRepository paiementRepository,
                           AlerteRepository alerteRepository) {
        this.reservationRepository = reservationRepository;
        this.abonnementsRepository = abonnementsRepository;
        this.vehiculesRepository = vehiculesRepository;
        this.paiementRepository = paiementRepository;
        this.alerteRepository = alerteRepository;
    }

    public Long idConnecte(Authentication auth) {
        return (auth != null && auth.getPrincipal() instanceof Clients c) ? c.getClient_id() : null;
    }

    public boolean estMoi(Long userId, Authentication auth) {
        Long moi = idConnecte(auth);
        return moi != null && moi.equals(userId);
    }

    public boolean proprietaireReservation(Long id, Authentication auth) {
        Long moi = idConnecte(auth);
        return moi != null && reservationRepository.findById(id)
                .map(r -> r.getClients().getClient_id().equals(moi)).orElse(false);
    }

    public boolean proprietaireAbonnement(Long id, Authentication auth) {
        Long moi = idConnecte(auth);
        return moi != null && abonnementsRepository.findById(id)
                .map(a -> a.getClients().getClient_id().equals(moi)).orElse(false);
    }

    public boolean proprietaireVehicule(Long id, Authentication auth) {
        Long moi = idConnecte(auth);
        return moi != null && vehiculesRepository.findById(id)
                .map(v -> v.getClients() != null && v.getClients().getClient_id().equals(moi)).orElse(false);
    }

    public boolean proprietairePaiement(Long id, Authentication auth) {
        Long moi = idConnecte(auth);
        return moi != null && paiementRepository.findById(id)
                .map(p -> p.getClients() != null && p.getClients().getClient_id().equals(moi)).orElse(false);
    }


    public boolean proprietaireAlerte(Long id, Authentication auth) {
        Long moi = idConnecte(auth);
        return moi != null && alerteRepository.findById(id)
                .map(a -> a.getClients() != null && a.getClients().getClient_id().equals(moi)).orElse(false);
    }


    public boolean peutCreerPaiement(TypePaiement type, Long cibleId, Authentication auth) {
        if (aRole(auth, "ROLE_ADMIN") || aRole(auth, "ROLE_BORNE")) {
            return true;
        }
        return switch (type) {
            case RESERVATION -> proprietaireReservation(cibleId, auth);
            case ABONNEMENT -> proprietaireAbonnement(cibleId, auth);
            case SESSION -> false;
        };
    }

    private boolean aRole(Authentication auth, String role) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }
}