package com.projet.parking.dto;

public record ZoneStatDTO(Long zoneId, String nom, int capaciteTotale, long nbPlaces,
                          long placesOccupees, long placesHorsService,
                          double tauxOccupation, double tauxRemplissage) {}