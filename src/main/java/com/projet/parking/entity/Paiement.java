package com.projet.parking.entity;

import com.projet.parking.enumeration.StatutPaiement;
import com.projet.parking.enumeration.TypePaiement;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Paiement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paiement_id;
    @Column(nullable = false)
    private BigDecimal montant;
    private BigDecimal montantRembourse;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutPaiement statut;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypePaiement typePaiement;
    @Column(nullable = false)
    private LocalDateTime dateCreation;
    private LocalDateTime dateValidation;


    @ToString.Exclude @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clients_id")
    private Clients clients;


    @ToString.Exclude @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entree_sortie_id")
    private EntreeSorties entreeSorties;

    @ToString.Exclude @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservations reservations;

    @ToString.Exclude @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "abonnement_id")
    private Abonnements abonnements;
}