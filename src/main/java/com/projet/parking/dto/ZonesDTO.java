package com.projet.parking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZonesDTO {
    private Long zone_id;
    @NotBlank(message = "Le nom de la zone est obligatoire")
    private String nom;
    private String description;
    @Min(value = 1, message = "La capacite totale doit etre au moins de 1")
    private int capaciteTotal;
}