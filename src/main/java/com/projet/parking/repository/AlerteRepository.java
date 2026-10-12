package com.projet.parking.repository;

import com.projet.parking.entity.Alerte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlerteRepository extends JpaRepository<Alerte, Long> {

    boolean existsByCle(String cle);

    @Query("select a from Alerte a where a.clients.client_id = :userId order by a.dateCreation desc")
    List<Alerte> findByDestinataire(@Param("userId") Long userId);

    @Query("select a from Alerte a where a.clients is null and a.lue = false order by a.dateCreation desc")
    List<Alerte> findAdministrateursNonLues();
}