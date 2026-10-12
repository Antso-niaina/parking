package com.projet.parking.repository;

import com.projet.parking.entity.Facture;
import com.projet.parking.enumeration.StatutPaiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FactureRepository extends JpaRepository<Facture, Long> {

    @Query("""
        select count(f) > 0 from Facture f
        where f.entreeSorties.vehicules.clients.client_id = :clientId
          and f.statutPaiement = :statut
    """)
    boolean existsImpayeByClientId(@Param("clientId") Long clientId,
                                   @Param("statut") StatutPaiement statut);
    @Query("select f from Facture f where f.entreeSorties.entreeSortie_id = :sessionId")
    Optional<Facture> findBySessionId(@Param("sessionId") Long sessionId);

    @Query("""
    select f from Facture f
    where f.entreeSorties.vehicules.clients.client_id = :clientId
      and f.statutPaiement = :statut
    order by f.dateFacturation
""")
    List<Facture> findByClientIdAndStatut(@Param("clientId") Long clientId,
                                          @Param("statut") StatutPaiement statut);
    @Query("""
    select f from Facture f
    where f.statutPaiement = :statut
      and f.entreeSorties.vehicules.clients is not null
""")
    List<Facture> findImpayeesAvecClient(@Param("statut") StatutPaiement statut);
}