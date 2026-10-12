package com.projet.parking.mapper;

import com.projet.parking.dto.ReservationsDTO;
import com.projet.parking.entity.Clients;
import com.projet.parking.entity.Places;
import com.projet.parking.entity.Reservations;

import java.time.LocalDateTime;

public class ReservationsMapper {

    public static ReservationsDTO toDto(Reservations r) {
        return new ReservationsDTO(
                r.getReservation_id(),
                r.getDate_heure_arrive(),
                r.getDate_heure_depart(),
                r.getDate_creation(),
                r.getStatut(),
                r.getClients() != null ? r.getClients().getClient_id() : null,
                r.getClients() != null ? r.getClients().getNom() : null,
                r.getVehicules() != null ? r.getVehicules().getVehicule_id() : null,
                r.getVehicules() != null ? r.getVehicules().getImmatriculation() : null,
                r.getPlaces() != null ? r.getPlaces().getPlace_id() : null,
                r.getPlaces() != null ? r.getPlaces().getNumero() : null
        );
    }
}