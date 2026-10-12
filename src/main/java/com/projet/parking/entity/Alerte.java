package com.projet.parking.entity;

import com.projet.parking.enumeration.TypeAlerte;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alerte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long alerte_id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeAlerte typeAlerte;
    @Column(nullable = false, length = 500)
    private String message;
    @Column(nullable = false)
    private LocalDateTime dateCreation;
    private boolean lue;

    // clé d'idempotence, ex. "RAPPEL_RESERVATION:12" : une même alerte n'est créée qu'une fois
    @Column(unique = true, length = 100)
    private String cle;

    // destinataire ; null = destinée aux administrateurs
    @ToString.Exclude @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clients_id")
    private Clients clients;
}