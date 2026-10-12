package com.projet.parking.enumeration;

public enum FormuleAbonnement {
    COMPLET,   // 24h/24, 7j/7
    JOUR,      // 06:00 - 20:00
    NUIT,      // 20:00 - 06:00 (à cheval sur minuit)
    WEEKEND    // samedi et dimanche
}