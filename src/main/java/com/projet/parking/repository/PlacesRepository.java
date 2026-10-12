package com.projet.parking.repository;

import com.projet.parking.entity.Places;
import com.projet.parking.enumeration.StatutPlace;
import com.projet.parking.enumeration.StatutReservation;
import com.projet.parking.enumeration.TypePlace;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PlacesRepository extends JpaRepository<Places, Long> {

    boolean existsByNumero(String numero);

    @Query("select count(p) from Places p where p.statut = :statut")
    long compterParStatut(@Param("statut") StatutPlace statut);

    @Query("select p.type_place, count(p) from Places p where p.statut = :statut group by p.type_place")
    List<Object[]> compterParTypeEtStatut(@Param("statut") StatutPlace statut);

    @Query("""
        select p from Places p
        where p.type_place in :types
          and p.statut <> :horsService
          and not exists (
              select 1 from Reservations r
              where r.places = p
                and r.statut in :statutsBloquants
                and r.date_heure_arrive < :fin
                and r.date_heure_depart > :debut)
          and not exists (
              select 1 from EntreeSorties e
              where e.places = p and e.dateHeureSortie is null)
        order by p.numero
    """)
    List<Places> trouverDisponibles(@Param("types") List<TypePlace> types,
                                    @Param("horsService") StatutPlace horsService,
                                    @Param("statutsBloquants") List<StatutReservation> statutsBloquants,
                                    @Param("debut") LocalDateTime debut,
                                    @Param("fin") LocalDateTime fin);

    /**
     * Charge les places d'un type en les verrouillant (SELECT ... FOR UPDATE) :
     * sérialise les souscriptions concurrentes pour que le contrôle du quota soit fiable.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Places p where p.type_place = :type")
    List<Places> findByTypeForUpdate(@Param("type") TypePlace type);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Places p where p.place_id = :id")
    Optional<Places> findByIdForUpdate(@Param("id") Long id);
    @Query("select count(p) from Places p where p.type_place = :type and p.statut <> :statut")
    long compterParTypeSaufStatut(@Param("type") TypePlace type, @Param("statut") StatutPlace statut);
    // PlacesRepository : à ajouter
    @Query("select count(p) from Places p where p.zones.zone_id = :zoneId")
    long compterParZone(@Param("zoneId") Long zoneId);

    @Query("select p from Places p where p.zones.zone_id = :zoneId order by p.numero")
    List<Places> findByZoneId(@Param("zoneId") Long zoneId);

    List<Places> findByZonesIsNull();

    // (zone, statut, nombre) : sert aux statistiques par zone
    @Query("""
    select p.zones.zone_id, p.statut, count(p) from Places p
    where p.zones is not null
    group by p.zones.zone_id, p.statut
""")
    List<Object[]> compterParZoneEtStatut();
}