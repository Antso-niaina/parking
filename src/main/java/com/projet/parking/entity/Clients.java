package com.projet.parking.entity;

import com.projet.parking.enumeration.ClientsEnum;
import com.projet.parking.enumeration.StatutUtilisateur;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Clients implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long client_id;
    @Column(nullable = false, length = 50)
    private String nom;
    @Column(nullable = false, length = 50)
    private String prenom;
    @Column(nullable = false, length = 50, unique = true)
    private String email;
    @Column(length = 13, nullable = false, unique = true)
    private String telephone;
    @Column(nullable = false)
    private String mdp;
    @Column(nullable = false)
    private LocalDate date_creation;
    @Enumerated(EnumType.STRING)
    private ClientsEnum type_client = ClientsEnum.STANDAR;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutUtilisateur statut = StatutUtilisateur.ACTIF;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "clients")
    List<Vehicules> vehicules = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "clients")
    List<Reservations> reservations = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "clients")
    List<Abonnements> abonnements = new ArrayList<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + type_client.name()));
    }

    @Override
    public String getPassword() {
        return mdp;
    }

    @Override
    public String getUsername() {
        return email;
    }
    @Override
    public boolean isAccountNonLocked() {
        return statut != StatutUtilisateur.SUSPENDU;
    }

    @Override
    public boolean isEnabled() {
        return statut != StatutUtilisateur.SUPPRIME;
    }
}