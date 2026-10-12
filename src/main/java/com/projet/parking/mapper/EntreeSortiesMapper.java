package com.projet.parking.mapper;

import com.projet.parking.dto.EntreeSortiesDTO;
import com.projet.parking.entity.EntreeSorties;
import com.projet.parking.entity.Vehicules;

public class EntreeSortiesMapper {

    public static EntreeSortiesDTO toDto(EntreeSorties entreeSorties) {
        return new EntreeSortiesDTO(
                entreeSorties.getEntreeSortie_id(),
                entreeSorties.getDateHeureEntree(),
                entreeSorties.getDateHeureSortie(),
                entreeSorties.getStatut(),
                entreeSorties.getVehicules().getVehicule_id(),
                entreeSorties.getVehicules().getImmatriculation()
        );
    }

    public static EntreeSorties toEntity(EntreeSortiesDTO dto, Vehicules vehicules) {
        EntreeSorties entity = new EntreeSorties();
        entity.setDateHeureEntree(dto.getDateHeureEntree());
        entity.setDateHeureSortie(dto.getDateHeureSortie());
        entity.setStatut(dto.getStatut());
        entity.setVehicules(vehicules);
        return entity;
    }
}