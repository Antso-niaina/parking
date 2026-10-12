package com.projet.parking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Zones {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long zone_id;
    @Column(nullable = false)
    private String nom;
    private String description;
    @Column(nullable = false)
    private int capaciteTotal;
    @OneToMany(mappedBy = "zones")
    List<Places>places = new ArrayList<>();
}
