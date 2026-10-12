package com.projet.parking.dto;

import com.projet.parking.enumeration.TypeVehicule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehiculesDTO {
    private Long vehicule_id;
    @NotBlank(message = "L'immatriculation est obligatoire")
    @Size(max = 10, message = "L'immatriculation ne doit pas depasser 10 caracteres")
    private String immatriculation;
    @NotNull(message = "Le type de vehicule est obligatoire")
    private TypeVehicule type_vehicule;
    @NotBlank(message = "La marque est obligatoire")
    private String marque;
    @NotBlank(message = "Le modele est obligatoire")
    private String model;
    @NotNull(message = "Le client est obligatoire")
    private Long client_id;
    private String clientNom;
}