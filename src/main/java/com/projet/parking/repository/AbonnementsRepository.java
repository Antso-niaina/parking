package com.projet.parking.repository;

import com.projet.parking.entity.Abonnements;
import com.projet.parking.enumeration.StatutAbonnement;
import com.projet.parking.enumeration.StatutPaiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AbonnementsRepository extends JpaRepository<Abonnements,Long> {
    @Query("""
    select count(a) > 0 from Abonnements a
    where a.clients.client_id = :clientId
      and a.statutAbonnement in :statuts
      and a.date_fin >= :maintenant
""")
    boolean existsActiveByClientId(@Param("clientId") Long clientId,
                                   @Param("statuts") List<StatutAbonnement> statuts,
                                   @Param("maintenant") LocalDateTime maintenant);


    @Query("""
    select count(a) > 0 from Abonnements a join a.vehicules v
    where v.vehicule_id = :vehiculeId
      and a.statutAbonnement in :statuts
      and a.date_debut < :fin
      and a.date_fin > :debut
""")
    boolean existsChevauchement(@Param("vehiculeId") Long vehiculeId,
                                @Param("statuts") List<StatutAbonnement> statuts,
                                @Param("debut") LocalDateTime debut,
                                @Param("fin") LocalDateTime fin);


    @Query("""
    select count(v) from Abonnements a join a.vehicules v
    where a.statutAbonnement in :statuts
      and a.date_debut < :fin
      and a.date_fin > :debut
""")
    long compterVehiculesAbonnes(@Param("statuts") List<StatutAbonnement> statuts,
                                 @Param("debut") LocalDateTime debut,
                                 @Param("fin") LocalDateTime fin);

    @Query("""
    select distinct a from Abonnements a join a.vehicules v
    where v.immatriculation = :immat
      and a.statutAbonnement in :statuts
      and a.statutPaiement = :paiement
      and a.date_debut <= :maintenant
      and a.date_fin > :maintenant
""")
    List<Abonnements> findValides(@Param("immat") String immatriculation,
                                  @Param("statuts") List<StatutAbonnement> statuts,
                                  @Param("paiement") StatutPaiement paiement,
                                  @Param("maintenant") LocalDateTime maintenant);

    @Query("""
    select a from Abonnements a
    where a.statutAbonnement in :statuts
      and a.date_fin <= :maintenant
""")
    List<Abonnements> findAExpirer(@Param("statuts") List<StatutAbonnement> statuts,
                                   @Param("maintenant") LocalDateTime maintenant);


    @Query("""
    select a from Abonnements a
    where a.statutAbonnement = :statut
      and a.date_debut <= :limite
""")
    List<Abonnements> findNonPayesAvant(@Param("statut") StatutAbonnement statut,
                                        @Param("limite") LocalDateTime limite);

    @Query("""
    select a from Abonnements a
    where a.statutAbonnement = :statut
      and a.alerteEnvoyee = false
      and a.date_fin > :maintenant
      and a.date_fin <= :limite
""")
    List<Abonnements> findAAlerter(@Param("statut") StatutAbonnement statut,
                                   @Param("maintenant") LocalDateTime maintenant,
                                   @Param("limite") LocalDateTime limite);
    @Query("select a from Abonnements a where a.clients.client_id = :userId order by a.date_debut desc")
    List<Abonnements> findByClientId(@Param("userId") Long userId);
}