package com.projet.parking.repository;

import com.projet.parking.entity.Paiement;
import com.projet.parking.enumeration.StatutPaiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PaiementRepository extends JpaRepository<Paiement, Long> {

    @Query("select count(p) > 0 from Paiement p where p.entreeSorties.entreeSortie_id = :id and p.statut in :statuts")
    boolean existsPourSession(@Param("id") Long id, @Param("statuts") List<StatutPaiement> statuts);

    @Query("select count(p) > 0 from Paiement p where p.reservations.reservation_id = :id and p.statut in :statuts")
    boolean existsPourReservation(@Param("id") Long id, @Param("statuts") List<StatutPaiement> statuts);

    @Query("select count(p) > 0 from Paiement p where p.abonnements.abonnement_id = :id and p.statut in :statuts")
    boolean existsPourAbonnement(@Param("id") Long id, @Param("statuts") List<StatutPaiement> statuts);


    @Query("select sum(p.montant) from Paiement p where p.entreeSorties.entreeSortie_id = :id and p.statut = :statut")
    BigDecimal totalParSession(@Param("id") Long id, @Param("statut") StatutPaiement statut);

    @Query("""
    select p from Paiement p
    where p.statut in :statuts
      and p.dateValidation >= :debut
      and p.dateValidation < :fin
""")
    List<Paiement> findEncaissesEntre(@Param("statuts") List<StatutPaiement> statuts,
                                      @Param("debut") LocalDateTime debut,
                                      @Param("fin") LocalDateTime fin);
    @Query("select p from Paiement p where p.clients.client_id = :userId order by p.dateCreation desc")
    List<Paiement> findByClientId(@Param("userId") Long userId);

    @Query("select p from Paiement p where p.reservations.reservation_id = :id and p.statut = :statut")
    List<Paiement> findByReservationIdAndStatut(@Param("id") Long id, @Param("statut") StatutPaiement statut);

    @Query("select p from Paiement p where p.abonnements.abonnement_id = :id and p.statut = :statut")
    List<Paiement> findByAbonnementIdAndStatut(@Param("id") Long id, @Param("statut") StatutPaiement statut);

    List<Paiement> findByStatutOrderByDateCreationDesc(StatutPaiement statut);
}