package com.projet.parking.dto;

import com.projet.parking.enumeration.TypePaiement;
import com.projet.parking.enumeration.TypePlace;

import java.math.BigDecimal;
import java.util.Map;

public record TableauDeBordDTO(
        double tauxOccupation,
        Map<TypePlace, Double> tauxOccupationParType,
        long sessionsActives,
        BigDecimal chiffreAffaires,
        Map<TypePaiement, BigDecimal> chiffreAffairesParType,
        long frequentation,
        long dureeMoyenneMinutes) {}