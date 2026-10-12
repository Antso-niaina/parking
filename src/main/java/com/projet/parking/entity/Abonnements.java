package com.projet.parking.entity;

import com.projet.parking.enumeration.FormuleAbonnement;
import com.projet.parking.enumeration.StatutAbonnement;
import com.projet.parking.enumeration.StatutPaiement;
import com.projet.parking.enumeration.TypeAbonnements;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Abonnements {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long abonnement_id;
    @Enumerated(EnumType.STRING)
    private TypeAbonnements type_abonnements;   // durée : JOURNALIER, HEBDOMADAIRE, MENSUEL, ANNUEL
    @Column(nullable = false)
    private LocalDateTime date_debut;
    @Column(nullable = false)
    private LocalDateTime date_fin;             // exclusive : valide tant que maintenant < date_fin
    @Enumerated(EnumType.STRING)
    private StatutAbonnement statutAbonnement;

    // --- nouveaux champs ---
    @Enumerated(EnumType.STRING)
    private FormuleAbonnement formule;          // plage horaire autorisée (null = COMPLET)
    @Enumerated(EnumType.STRING)
    private StatutPaiement statutPaiement;
    private BigDecimal montantPaye;
    private boolean renouvellementAuto;
    private boolean alerteEnvoyee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clients_id")
    private Clients clients;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToMany
    @JoinTable(name = "abonnements_vehicules",
            joinColumns = @JoinColumn(name = "abonnement_id"),
            inverseJoinColumns = @JoinColumn(name = "vehicule_id"))
    private List<Vehicules> vehicules = new ArrayList<>();
}