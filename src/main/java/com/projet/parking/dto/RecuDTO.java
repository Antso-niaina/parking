package com.projet.parking.dto;

import com.projet.parking.enumeration.StatutPaiement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RecuDTO(String numero, LocalDateTime date, String client, String objet,
                      BigDecimal montant, BigDecimal montantRembourse, StatutPaiement statut) {

}