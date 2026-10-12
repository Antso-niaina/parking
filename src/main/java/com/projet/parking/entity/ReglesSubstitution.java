package com.projet.parking.entity;

import com.projet.parking.enumeration.TypePlace;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReglesSubstitution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long regle_id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypePlace typePlaceDemande;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypePlace typePlaceSubstitut;
    @Column(nullable = false)
    private int ordrePriorite;
}
