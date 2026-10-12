package com.projet.parking.dto;

import com.projet.parking.entity.Vehicules;
import com.projet.parking.enumeration.StatutSortie;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntreeSortiesDTO {
    private Long entreeSortie_id;
    @NotNull(message = "Le date d'entree doit etre obligatoire")
    private LocalDateTime dateHeureEntree;
    private LocalDateTime dateHeureSortie;
    @NotNull(message = "Le statut de sortie doit etre obligatoire")
    private StatutSortie statut;
    @NotNull(message = "Le vehicule doit etre obligatoire")
    private Long vehicule_id;
    @NotBlank(message = "L'immatriculation est obligatoire")
    private String immatriculation;
}
