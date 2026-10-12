package com.projet.parking.dto;

import com.projet.parking.enumeration.StatutPlace;
import com.projet.parking.enumeration.TypePlace;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlacesDTO {
    private Long place_id;
    @NotBlank(message = "Le numero de la place est obligatoire")
    private String numero;
    @NotNull(message = "Le type de place est obligatoire")
    private TypePlace type_place;
    private StatutPlace statut;
    @NotNull(message = "La zone est obligatoire")
    private Long zone_id;
    private String zoneNom;
}