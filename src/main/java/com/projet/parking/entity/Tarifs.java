package com.projet.parking.entity;

import com.projet.parking.enumeration.TypePlace;
import com.projet.parking.enumeration.TypeVehicule;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Tarifs {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tarif_id;
    @Column(nullable = false)
    private BigDecimal tarifHoraire;
    @Column(nullable = false)
    private BigDecimal forfaitJournalier;
    @Column(nullable = false)
    private LocalDateTime dateDebutValidite;
    @Column(nullable = false)
    private LocalDateTime dateFinValidite;
    @Column(nullable = false)
    private double reductionAbonnePourcentage;
    private boolean actif;
    private int franchiseMinutes;
    private double majorationNuitPourcentage;
    private double majorationWeekendPourcentage;
    private double reductionPmrPourcentage;
    private double reductionVipPourcentage;
    private BigDecimal penaliteDepassementHoraire;
    private BigDecimal penaliteNoShow;
    private BigDecimal penaliteTicketPerdu;
    private BigDecimal penaliteMauvaisePlace;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tarif_coef_vehicule", joinColumns = @JoinColumn(name = "tarif_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "type_vehicule")
    @Column(name = "coef")
    private Map<TypeVehicule, BigDecimal> coefVehicule = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tarif_coef_place", joinColumns = @JoinColumn(name = "tarif_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "type_place")
    @Column(name = "coef")
    private Map<TypePlace, BigDecimal> coefPlace = new HashMap<>();
    @OneToMany(mappedBy = "tarifs")
    List<Facture> factures = new ArrayList<>();

}
