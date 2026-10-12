package com.projet.parking.repository;

import com.projet.parking.entity.Tarifs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TarifsRepository extends JpaRepository<Tarifs, Long> {

    @Query("""
    select t from Tarifs t
    where t.actif = true
      and t.dateDebutValidite <= :maintenant
      and t.dateFinValidite > :maintenant
    order by t.dateDebutValidite desc
""")
    List<Tarifs> findActifs(@Param("maintenant") LocalDateTime maintenant);

    @Query("""
    select t from Tarifs t
    where t.dateDebutValidite <= :instant
      and t.dateFinValidite > :instant
    order by t.dateDebutValidite desc
""")
    List<Tarifs> findEnVigueur(@Param("instant") LocalDateTime instant);

    @Query("select t from Tarifs t where t.dateFinValidite > :maintenant")
    List<Tarifs> findNonTermines(@Param("maintenant") LocalDateTime maintenant);

    List<Tarifs> findAllByOrderByDateDebutValiditeDesc();
}