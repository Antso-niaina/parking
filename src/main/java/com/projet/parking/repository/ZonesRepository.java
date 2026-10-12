// ZonesRepository : remplace tout le fichier
package com.projet.parking.repository;

import com.projet.parking.entity.Zones;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ZonesRepository extends JpaRepository<Zones, Long> {

    boolean existsByNomIgnoreCase(String nom);

    List<Zones> findAllByOrderByNomAsc();

    // même nom, mais sur une autre zone que celle qu'on modifie
    @Query("select count(z) > 0 from Zones z where lower(z.nom) = lower(:nom) and z.zone_id <> :id")
    boolean existsNomPourAutre(@Param("nom") String nom, @Param("id") Long id);

    // verrou : deux admins ne peuvent pas dépasser la capacité en même temps
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select z from Zones z where z.zone_id = :id")
    Optional<Zones> findByIdForUpdate(@Param("id") Long id);
}