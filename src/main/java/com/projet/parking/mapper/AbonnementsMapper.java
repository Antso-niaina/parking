package com.projet.parking.mapper;

import com.projet.parking.dto.AbonnementsDTO;
import com.projet.parking.entity.Abonnements;
import com.projet.parking.entity.Clients;
import com.projet.parking.entity.Vehicules;

public class AbonnementsMapper {

    public static AbonnementsDTO toDto(Abonnements abonnements) {
        return new AbonnementsDTO(
                abonnements.getAbonnement_id(),
                abonnements.getType_abonnements(),
                abonnements.getDate_debut(),
                abonnements.getDate_fin(),
                abonnements.getStatutAbonnement(),
                abonnements.getClients().getClient_id(),
                abonnements.getClients().getNom(),
                abonnements.getFormule(),
                abonnements.getStatutPaiement(),
                abonnements.getMontantPaye(),
                abonnements.isRenouvellementAuto(),
                abonnements.getVehicules().stream().map(Vehicules::getImmatriculation).toList()
        );
    }

    public static Abonnements toEntity(AbonnementsDTO dto, Clients clients) {
        Abonnements entity = new Abonnements();
        entity.setType_abonnements(dto.getType_abonnements());
        entity.setDate_debut(dto.getDate_debut());
        entity.setDate_fin(dto.getDate_fin());
        entity.setStatutAbonnement(dto.getStatutAbonnement());
        entity.setFormule(dto.getFormule());
        entity.setRenouvellementAuto(dto.isRenouvellementAuto());
        entity.setClients(clients);
        return entity;
    }
}