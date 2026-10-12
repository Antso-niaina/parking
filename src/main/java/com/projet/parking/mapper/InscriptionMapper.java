package com.projet.parking.mapper;

import com.projet.parking.dto.InscriptionDTO;
import com.projet.parking.entity.Clients;

import java.time.LocalDate;
import java.util.ArrayList;

public class InscriptionMapper {

    public static Clients toEntity(InscriptionDTO dto, String mdpEncode) {
        Clients entity = new Clients();
        entity.setNom(dto.getNom());
        entity.setPrenom(dto.getPrenom());
        entity.setEmail(dto.getEmail());
        entity.setTelephone(dto.getTelephone());
        entity.setMdp(mdpEncode);
        entity.setDate_creation(LocalDate.now());
        entity.setVehicules(new ArrayList<>());
        entity.setReservations(new ArrayList<>());
        entity.setAbonnements(new ArrayList<>());
        return entity;
    }
}