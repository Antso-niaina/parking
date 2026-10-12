package com.projet.parking.dto;

import com.projet.parking.enumeration.StatutPaiement;
import com.projet.parking.enumeration.TypePaiement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaiementDTO {
    private Long paiement_id;
    private BigDecimal montant;
    private BigDecimal montantRembourse;
    private StatutPaiement statut;
    private TypePaiement typePaiement;
    private LocalDateTime dateCreation;
    private LocalDateTime dateValidation;
    private Long clientId;
    private Long cibleId;
}