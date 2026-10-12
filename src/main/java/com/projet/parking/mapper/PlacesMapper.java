package com.projet.parking.mapper;

import com.projet.parking.dto.PlacesDTO;
import com.projet.parking.entity.Places;
import com.projet.parking.entity.Zones;

public class PlacesMapper {

    public static PlacesDTO toDto(Places places) {
        return new PlacesDTO(
                places.getPlace_id(),
                places.getNumero(),
                places.getType_place(),
                places.getStatut(),
                places.getZones() != null ? places.getZones().getZone_id() : null,
                places.getZones() != null ? places.getZones().getNom() : null
        );
    }

    public static Places toEntity(PlacesDTO dto, Zones zones) {
        Places entity = new Places();
        entity.setNumero(dto.getNumero());
        entity.setType_place(dto.getType_place());
        entity.setStatut(dto.getStatut());
        entity.setZones(zones);
        return entity;
    }
}