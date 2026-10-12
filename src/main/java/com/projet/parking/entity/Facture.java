package com.projet.parking.entity;

import com.projet.parking.enumeration.StatutPaiement;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Facture {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long facture_id;
    private int dureePrevueMinute;
    private int dureeReelleMinute;
    private int dureeDepassementMinutes;
    @Column(nullable = false)
    private BigDecimal montantBase;
    private BigDecimal montantMajoration;
    private BigDecimal montantReduction;
    private BigDecimal montantTotal;
    @Enumerated(EnumType.STRING)
    private StatutPaiement statutPaiement;
    @Column(nullable = false)
    private LocalDate dateFacturation;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreeSorties_id")
    private EntreeSorties entreeSorties;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarif_id")
    private Tarifs tarifs;
}
