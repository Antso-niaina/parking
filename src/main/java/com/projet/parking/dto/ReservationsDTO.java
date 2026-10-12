package com.projet.parking.dto;

import com.projet.parking.enumeration.StatutReservation;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationsDTO {
    private Long reservation_id;
    @NotNull(message = "La date d'arrivee est obligatoire")
    private LocalDateTime date_heure_arrive;
    @NotNull(message = "La date de depart est obligatoire")
    private LocalDateTime date_heure_depart;
    private LocalDateTime date_creation;
    private StatutReservation statut;
    private Long client_id;
    private String clientNom;
    @NotNull(message = "Le vehicule est obligatoire")
    private Long vehicule_id;
    private String immatriculation;
    @NotNull(message = "La place est obligatoire")
    private Long place_id;
    private String placeNumero;
}