package com.projet.parking.entity;

import com.projet.parking.enumeration.TypeVehicule;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vehicules {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long vehicule_id;

    @Column(nullable = false, length = 10, unique = true)
    private String immatriculation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeVehicule type_vehicule;

    @Column(nullable = false)
    private String marque;

    @Column(nullable = false)
    private String model;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clients_id")
    private Clients clients;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "vehicules")
    List<EntreeSorties> entreeSorties = new ArrayList<>();
}