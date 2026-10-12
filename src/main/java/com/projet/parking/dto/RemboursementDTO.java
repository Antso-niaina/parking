package com.projet.parking.dto;

import java.math.BigDecimal;

// dto/RemboursementDTO.java
public record RemboursementDTO(BigDecimal montantRembourse, Long paiementId) {}
