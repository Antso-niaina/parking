package com.projet.parking.mapper;

import com.projet.parking.dto.VehiculesDTO;
import com.projet.parking.entity.Clients;
import com.projet.parking.entity.Vehicules;

public class VehiculesMapper {

    public static VehiculesDTO toDto(Vehicules vehicules) {
        return new VehiculesDTO(
                vehicules.getVehicule_id(),
                vehicules.getImmatriculation(),
                vehicules.getType_vehicule(),
                vehicules.getMarque(),
                vehicules.getModel(),
                vehicules.getClients() != null ? vehicules.getClients().getClient_id() : null,
                vehicules.getClients() != null ? vehicules.getClients().getNom() : null
        );
    }

    public static Vehicules toEntity(VehiculesDTO dto, Clients clients) {
        Vehicules entity = new Vehicules();
        entity.setImmatriculation(dto.getImmatriculation());
        entity.setType_vehicule(dto.getType_vehicule());
        entity.setMarque(dto.getMarque());
        entity.setModel(dto.getModel());
        entity.setClients(clients);
        return entity;
    }
    public static void updateEntity(Vehicules entity, VehiculesDTO dto) {
        entity.setImmatriculation(dto.getImmatriculation());
        entity.setType_vehicule(dto.getType_vehicule());
        entity.setMarque(dto.getMarque());
        entity.setModel(dto.getModel());
    }
}