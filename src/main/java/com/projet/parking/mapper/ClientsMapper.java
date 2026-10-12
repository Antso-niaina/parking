package com.projet.parking.mapper;

import com.projet.parking.dto.ClientsDTO;
import com.projet.parking.entity.Clients;

import java.time.LocalDate;
import java.util.ArrayList;

public class ClientsMapper {

    public static ClientsDTO toDto(Clients clients) {
        return new ClientsDTO(
                clients.getClient_id(),
                clients.getNom(),
                clients.getPrenom(),
                clients.getEmail(),
                clients.getTelephone()
        );
    }

    public static Clients toEntity(ClientsDTO dto) {
        Clients entity = new Clients();
        entity.setNom(dto.getNom());
        entity.setPrenom(dto.getPrenom());
        entity.setEmail(dto.getEmail());
        entity.setTelephone(dto.getTelephone());
        entity.setDate_creation(LocalDate.now());
        entity.setVehicules(new ArrayList<>());
        entity.setReservations(new ArrayList<>());
        entity.setAbonnements(new ArrayList<>());
        return entity;
    }
    public static void updateEntity(Clients entity, ClientsDTO dto) {
        entity.setNom(dto.getNom());
        entity.setPrenom(dto.getPrenom());
        entity.setEmail(dto.getEmail());
        entity.setTelephone(dto.getTelephone());
    }
}