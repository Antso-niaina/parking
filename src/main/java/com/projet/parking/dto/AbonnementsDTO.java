package com.projet.parking.dto;

import com.projet.parking.enumeration.FormuleAbonnement;
import com.projet.parking.enumeration.StatutAbonnement;
import com.projet.parking.enumeration.StatutPaiement;
import com.projet.parking.enumeration.TypeAbonnements;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AbonnementsDTO {
    private Long abonnement_id;
    @NotNull(message = "Le type d'abonnement est obligatoire")
    private TypeAbonnements type_abonnements;
    @NotNull(message = "Le date du debut est obligatoire")
    private LocalDateTime date_debut;
    @NotNull(message = "Le date du fin est obligatoire")
    private LocalDateTime date_fin;
    @NotNull(message = "Le Statut d'abonnements")
    private StatutAbonnement statutAbonnement;
    @NotNull(message = "L'id du Client est obligatoire")
    private Long clientId;
    @NotBlank(message = "Le nom du Client est obligatoire")
    private String clientNom;
    private FormuleAbonnement formule;
    private StatutPaiement statutPaiement;
    private BigDecimal montantPaye;
    private boolean renouvellementAuto;
    private List<String> immatriculations;
}