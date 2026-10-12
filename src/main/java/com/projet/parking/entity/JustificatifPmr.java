package com.projet.parking.entity;

import com.projet.parking.enumeration.StatutJustificatif;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JustificatifPmr {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long justificatif_id;
    @Column(nullable = false, unique = true, length = 50)
    private String numeroCarte;
    private LocalDate dateExpiration;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutJustificatif statut = StatutJustificatif.EN_ATTENTE;
    @Column(nullable = false)
    private LocalDateTime dateDepot;
    private LocalDateTime dateVerification;

    @ToString.Exclude @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clients_id")
    private Clients clients;
}
