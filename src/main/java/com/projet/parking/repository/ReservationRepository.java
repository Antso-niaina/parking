package com.projet.parking.repository;

import com.projet.parking.entity.Reservations;
import com.projet.parking.enumeration.StatutReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservations, Long> {

    @Query("""
        select count(r) > 0 from Reservations r
        where r.clients.client_id = :clientId and r.statut in :statuts
    """)
    boolean existsActiveByClientId(@Param("clientId") Long clientId,
                                   @Param("statuts") List<StatutReservation> statuts);

    @Query("""
        select count(r) > 0 from Reservations r
        where r.places.place_id = :placeId
          and r.reservation_id <> :excludeId
          and r.statut in :statuts
          and r.date_heure_arrive < :fin
          and r.date_heure_depart > :debut
    """)
    boolean existsChevauchementPlace(@Param("placeId") Long placeId,
                                     @Param("excludeId") Long excludeId,
                                     @Param("statuts") List<StatutReservation> statuts,
                                     @Param("debut") LocalDateTime debut,
                                     @Param("fin") LocalDateTime fin);

    @Query("""
        select count(r) > 0 from Reservations r
        where r.vehicules.vehicule_id = :vehiculeId
          and r.reservation_id <> :excludeId
          and r.statut in :statuts
          and r.date_heure_arrive < :fin
          and r.date_heure_depart > :debut
    """)
    boolean existsChevauchementVehicule(@Param("vehiculeId") Long vehiculeId,
                                        @Param("excludeId") Long excludeId,
                                        @Param("statuts") List<StatutReservation> statuts,
                                        @Param("debut") LocalDateTime debut,
                                        @Param("fin") LocalDateTime fin);

    @Query("""
        select count(r) > 0 from Reservations r
        where r.places.place_id = :placeId
          and r.statut in :statuts
          and r.date_heure_depart > :maintenant
    """)
    boolean existsReservationEnCoursOuAVenir(@Param("placeId") Long placeId,
                                             @Param("statuts") List<StatutReservation> statuts,
                                             @Param("maintenant") LocalDateTime maintenant);

    @Query("""
        select r from Reservations r
        where r.vehicules.immatriculation = :immat
          and r.statut = :statut
          and r.date_heure_arrive <= :limiteArrivee
          and r.date_heure_depart > :maintenant
        order by r.date_heure_arrive
    """)
    List<Reservations> findAccesValides(@Param("immat") String immat,
                                        @Param("statut") StatutReservation statut,
                                        @Param("limiteArrivee") LocalDateTime limiteArrivee,
                                        @Param("maintenant") LocalDateTime maintenant);

    @Query("select r from Reservations r where r.statut = :statut and r.date_heure_arrive < :limite")
    List<Reservations> findNoShows(@Param("statut") StatutReservation statut,
                                   @Param("limite") LocalDateTime limite);

    @Query("""
    select r from Reservations r
    where r.statut = :statut
      and r.date_heure_arrive > :maintenant
      and r.date_heure_arrive <= :limite
""")
    List<Reservations> findARappeler(@Param("statut") StatutReservation statut,
                                     @Param("maintenant") LocalDateTime maintenant,
                                     @Param("limite") LocalDateTime limite);
    @Query("select r from Reservations r where r.clients.client_id = :userId order by r.date_heure_arrive desc")
    List<Reservations> findByClientId(@Param("userId") Long userId);
}