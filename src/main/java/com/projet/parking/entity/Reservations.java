package com.projet.parking.entity;

import com.projet.parking.enumeration.StatutReservation;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Reservations {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reservation_id;
    @Column(nullable = false)
    private LocalDateTime date_heure_arrive;
    @Column(nullable = false)
    private LocalDateTime date_heure_depart;
    @Column(nullable = false)
    private LocalDateTime date_creation;
    @Enumerated(EnumType.STRING)
    private StatutReservation statut;
    private BigDecimal montantPaye;
    private BigDecimal montantPenalite;

    @ToString.Exclude @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clients_id")
    private Clients clients;

    @ToString.Exclude @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicules_id")
    private Vehicules vehicules;

    @ToString.Exclude @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Places places;
}