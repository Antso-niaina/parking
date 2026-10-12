package com.projet.parking.mapper;

import com.projet.parking.dto.FactureDTO;
import com.projet.parking.entity.Facture;

public class FactureMapper {

    public static FactureDTO toDto(Facture facture) {
        return new FactureDTO(
                facture.getFacture_id(),
                facture.getDureePrevueMinute(),
                facture.getDureeReelleMinute(),
                facture.getDureeDepassementMinutes(),
                facture.getMontantBase(),
                facture.getMontantMajoration(),
                facture.getMontantReduction(),
                facture.getMontantTotal(),
                facture.getStatutPaiement(),
                facture.getDateFacturation(),
                facture.getEntreeSorties() != null ? facture.getEntreeSorties().getEntreeSortie_id() : null,
                facture.getTarifs() != null ? facture.getTarifs().getTarif_id() : null
        );
    }
}