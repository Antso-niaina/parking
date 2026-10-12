package com.projet.parking.mapper;

import com.projet.parking.dto.PaiementDTO;
import com.projet.parking.entity.Paiement;

public class PaiementMapper {
    public static PaiementDTO toDto(Paiement p) {
        Long cible = switch (p.getTypePaiement()) {
            case SESSION -> p.getEntreeSorties().getEntreeSortie_id();
            case RESERVATION -> p.getReservations().getReservation_id();
            case ABONNEMENT -> p.getAbonnements().getAbonnement_id();
        };
        return new PaiementDTO(
                p.getPaiement_id(),
                p.getMontant(),
                p.getMontantRembourse(),
                p.getStatut(),
                p.getTypePaiement(),
                p.getDateCreation(),
                p.getDateValidation(),
                p.getClients() != null ? p.getClients().getClient_id() : null,
                cible
        );
    }
}