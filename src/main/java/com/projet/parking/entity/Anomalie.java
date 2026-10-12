package com.projet.parking.entity;

import com.projet.parking.enumeration.TypeAnomalie;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Anomalie {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long anomalie_id;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TypeAnomalie typeAnomalie;
    @Column(length = 500)
    private String details;
    @Column(nullable = false)
    private LocalDateTime dateHeure;
}