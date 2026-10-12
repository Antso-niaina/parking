package com.projet.parking.repository;

import com.projet.parking.entity.Vehicules;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehiculesRepository extends JpaRepository<Vehicules, Long> {

    boolean existsByImmatriculation(String immatriculation);
    Optional<Vehicules> findByImmatriculation(String immatriculation);

    @Query("select count(v) > 0 from Vehicules v where v.immatriculation = :immat and v.vehicule_id <> :id")
    boolean existsByImmatriculationAndIdNot(@Param("immat") String immatriculation, @Param("id") Long id);

    @Query("select v from Vehicules v where v.clients.client_id = :userId")
    List<Vehicules> findByClientId(@Param("userId") Long userId);
}