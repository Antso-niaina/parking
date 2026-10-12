package com.projet.parking.mapper;

import com.projet.parking.dto.ZonesDTO;
import com.projet.parking.entity.Zones;

public class ZonesMapper {

    public static ZonesDTO toDto(Zones zones) {
        return new ZonesDTO(
                zones.getZone_id(),
                zones.getNom(),
                zones.getDescription(),
                zones.getCapaciteTotal()
        );
    }

    public static Zones toEntity(ZonesDTO dto) {
        Zones entity = new Zones();
        entity.setNom(dto.getNom());
        entity.setDescription(dto.getDescription());
        entity.setCapaciteTotal(dto.getCapaciteTotal());
        return entity;
    }
}