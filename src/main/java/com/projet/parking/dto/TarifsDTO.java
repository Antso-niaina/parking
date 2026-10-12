package com.projet.parking.dto;

import com.projet.parking.enumeration.TypePlace;
import com.projet.parking.enumeration.TypeVehicule;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TarifsDTO {
    private Long tarif_id;
    @NotNull(message = "Le tarif horaire est obligatoire")
    @DecimalMin(value = "0.0", message = "Le tarif horaire doit etre positif")
    private BigDecimal tarifHoraire;
    @NotNull(message = "Le forfait journalier est obligatoire")
    @DecimalMin(value = "0.0", message = "Le forfait journalier doit etre positif")
    private BigDecimal forfaitJournalier;
    @NotNull(message = "La date de debut de validite est obligatoire")
    private LocalDateTime dateDebutValidite;
    @NotNull(message = "La date de fin de validite est obligatoire")
    private LocalDateTime dateFinValidite;
    @DecimalMin(value = "0.0", message = "La reduction doit etre comprise entre 0 et 100")
    @DecimalMax(value = "100.0", message = "La reduction doit etre comprise entre 0 et 100")
    private double reductionAbonnePourcentage;
    private boolean actif;

    // --- nouveaux champs de la grille ---
    @Min(value = 0, message = "La franchise ne peut pas etre negative")
    private int franchiseMinutes;
    @DecimalMin(value = "0.0") @DecimalMax(value = "100.0")
    private double majorationNuitPourcentage;
    @DecimalMin(value = "0.0") @DecimalMax(value = "100.0")
    private double majorationWeekendPourcentage;
    @DecimalMin(value = "0.0") @DecimalMax(value = "100.0")
    private double reductionPmrPourcentage;
    @DecimalMin(value = "0.0") @DecimalMax(value = "100.0")
    private double reductionVipPourcentage;
    @DecimalMin(value = "0.0", message = "Une penalite ne peut pas etre negative")
    private BigDecimal penaliteDepassementHoraire;
    @DecimalMin(value = "0.0", message = "Une penalite ne peut pas etre negative")
    private BigDecimal penaliteNoShow;
    @DecimalMin(value = "0.0", message = "Une penalite ne peut pas etre negative")
    private BigDecimal penaliteTicketPerdu;
    @DecimalMin(value = "0.0", message = "Une penalite ne peut pas etre negative")
    private BigDecimal penaliteMauvaisePlace;
    private Map<TypeVehicule, BigDecimal> coefVehicule = new HashMap<>();
    private Map<TypePlace, BigDecimal> coefPlace = new HashMap<>();
}