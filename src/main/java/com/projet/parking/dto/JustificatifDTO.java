package com.projet.parking.dto;

import com.projet.parking.enumeration.StatutJustificatif;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record JustificatifDTO(Long justificatifId, String numeroCarte, LocalDate dateExpiration,
                              StatutJustificatif statut, Long clientId, String clientNom,
                              LocalDateTime dateDepot) {}
