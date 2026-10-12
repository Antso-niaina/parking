package com.projet.parking.entity;

import com.projet.parking.enumeration.StatutPlace;
import com.projet.parking.enumeration.TypePlace;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Places {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long place_id;

    @Column(nullable = false, unique = true)
    private String numero;

    private Integer niveau;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypePlace type_place;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutPlace statut = StatutPlace.LIBRE;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "places")
    List<Reservations> reservations = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "places")
    List<EntreeSorties> entreeSorties = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zones_id")
    private Zones zones;
}