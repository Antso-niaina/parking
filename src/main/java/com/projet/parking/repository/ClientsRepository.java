package com.projet.parking.repository;

import com.projet.parking.entity.Clients;
import com.projet.parking.enumeration.ClientsEnum;
import com.projet.parking.enumeration.StatutReservation;
import com.projet.parking.enumeration.StatutUtilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClientsRepository extends JpaRepository<Clients,Long> {
    Optional<Clients> findByEmail(String email);
    @Query("select c from Clients c where c.type_client = :type and c.statut = :statut")
    List<Clients> findByTypeAndStatut(@Param("type") ClientsEnum type, @Param("statut") StatutUtilisateur statut);
}
