package com.projet.parking.dto;

import com.projet.parking.enumeration.StatutPaiement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FactureDTO {
    private Long facture_id;
    private int dureePrevueMinute;
    private int dureeReelleMinute;
    private int dureeDepassementMinutes;
    private BigDecimal montantBase;
    private BigDecimal montantMajoration;
    private BigDecimal montantReduction;
    private BigDecimal montantTotal;
    private StatutPaiement statutPaiement;
    private LocalDate dateFacturation;
    private Long entreeSortie_id;
    private Long tarif_id;
}