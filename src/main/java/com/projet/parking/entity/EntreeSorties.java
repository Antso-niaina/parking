package com.projet.parking.entity;

import com.projet.parking.enumeration.StatutSortie;
import com.projet.parking.enumeration.TypeAcces;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EntreeSorties {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long entreeSortie_id;
    @Column(nullable = false)
    private LocalDateTime dateHeureEntree;
    private LocalDateTime dateHeureSortie;
    @Enumerated(EnumType.STRING)
    private StatutSortie statut;
    private boolean placeRepli;
    private boolean occupationSignalee;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicules_id")
    private Vehicules vehicules;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "places_id")
    private Places places;
    @Enumerated(EnumType.STRING)
    private TypeAcces typeAcces;
    private boolean ticketPerdu;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservations_id")
    private Reservations reservation;
}
