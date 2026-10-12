package com.projet.parking.mapper;

import com.projet.parking.dto.TarifsDTO;
import com.projet.parking.entity.Tarifs;

import java.util.HashMap;

public class TarifsMapper {

    public static TarifsDTO toDto(Tarifs tarifs) {
        return new TarifsDTO(
                tarifs.getTarif_id(),
                tarifs.getTarifHoraire(),
                tarifs.getForfaitJournalier(),
                tarifs.getDateDebutValidite(),
                tarifs.getDateFinValidite(),
                tarifs.getReductionAbonnePourcentage(),
                tarifs.isActif(),
                tarifs.getFranchiseMinutes(),
                tarifs.getMajorationNuitPourcentage(),
                tarifs.getMajorationWeekendPourcentage(),
                tarifs.getReductionPmrPourcentage(),
                tarifs.getReductionVipPourcentage(),
                tarifs.getPenaliteDepassementHoraire(),
                tarifs.getPenaliteNoShow(),
                tarifs.getPenaliteTicketPerdu(),
                tarifs.getPenaliteMauvaisePlace(),
                new HashMap<>(tarifs.getCoefVehicule()),
                new HashMap<>(tarifs.getCoefPlace())
        );
    }

    public static Tarifs toEntity(TarifsDTO dto) {
        Tarifs entity = new Tarifs();
        entity.setTarifHoraire(dto.getTarifHoraire());
        entity.setForfaitJournalier(dto.getForfaitJournalier());
        entity.setDateDebutValidite(dto.getDateDebutValidite());
        entity.setDateFinValidite(dto.getDateFinValidite());
        entity.setReductionAbonnePourcentage(dto.getReductionAbonnePourcentage());
        entity.setActif(dto.isActif());

        entity.setFranchiseMinutes(dto.getFranchiseMinutes());
        entity.setMajorationNuitPourcentage(dto.getMajorationNuitPourcentage());
        entity.setMajorationWeekendPourcentage(dto.getMajorationWeekendPourcentage());
        entity.setReductionPmrPourcentage(dto.getReductionPmrPourcentage());
        entity.setReductionVipPourcentage(dto.getReductionVipPourcentage());
        entity.setPenaliteDepassementHoraire(dto.getPenaliteDepassementHoraire());
        entity.setPenaliteNoShow(dto.getPenaliteNoShow());
        entity.setPenaliteTicketPerdu(dto.getPenaliteTicketPerdu());
        entity.setPenaliteMauvaisePlace(dto.getPenaliteMauvaisePlace());
        entity.setCoefVehicule(dto.getCoefVehicule() != null ? new HashMap<>(dto.getCoefVehicule()) : new HashMap<>());
        entity.setCoefPlace(dto.getCoefPlace() != null ? new HashMap<>(dto.getCoefPlace()) : new HashMap<>());
        return entity;
    }
}