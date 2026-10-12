package com.projet.parking.mapper;

import com.projet.parking.dto.AlerteDTO;
import com.projet.parking.entity.Alerte;

public class AlerteMapper {
    public static AlerteDTO toDto(Alerte a) {
        return new AlerteDTO(
                a.getAlerte_id(),
                a.getTypeAlerte(),
                a.getMessage(),
                a.getDateCreation(),
                a.isLue(),
                a.getClients() != null ? a.getClients().getClient_id() : null
        );
    }
}