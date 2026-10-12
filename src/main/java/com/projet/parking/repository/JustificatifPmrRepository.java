package com.projet.parking.repository;

import com.projet.parking.entity.JustificatifPmr;
import com.projet.parking.enumeration.StatutJustificatif;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface JustificatifPmrRepository extends JpaRepository<JustificatifPmr, Long> {

    boolean existsByNumeroCarte(String numeroCarte);

    @Query("""
        select count(j) > 0 from JustificatifPmr j
        where j.clients.client_id = :clientId
          and j.statut = :statut
          and (j.dateExpiration is null or j.dateExpiration >= :aujourdhui)
    """)
    boolean existsValide(@Param("clientId") Long clientId,
                         @Param("statut") StatutJustificatif statut,
                         @Param("aujourdhui") LocalDate aujourdhui);

    @Query("select j from JustificatifPmr j where j.statut = :statut order by j.dateDepot")
    List<JustificatifPmr> findByStatut(@Param("statut") StatutJustificatif statut);

    @Query("select j from JustificatifPmr j where j.clients.client_id = :userId order by j.dateDepot desc")
    List<JustificatifPmr> findByClientId(@Param("userId") Long userId);
}