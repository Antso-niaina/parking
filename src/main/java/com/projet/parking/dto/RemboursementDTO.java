package com.projet.parking.dto;

import java.math.BigDecimal;


public record RemboursementDTO(BigDecimal montantRembourse, Long paiementId) {}
