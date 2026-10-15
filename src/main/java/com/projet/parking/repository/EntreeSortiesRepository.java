package com.projet.parking.repository;

import com.projet.parking.entity.EntreeSorties;
import com.projet.parking.enumeration.StatutSortie;
import com.projet.parking.enumeration.TypePlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EntreeSortiesRepository extends JpaRepository<EntreeSorties, Long> {

    @Query("""
        select count(e) > 0 from EntreeSorties e
        where e.vehicules.clients.client_id = :clientId
          and e.dateHeureSortie is null
    """)
    boolean existsActiveByClientId(@Param("clientId") Long clientId);

    @Query("""
    select count(e) > 0 from EntreeSorties e
    where e.vehicules.immatriculation = :immat
      and e.dateHeureSortie is null
""")
    boolean existsPresentByImmatriculation(@Param("immat") String immatriculation);

    @Query("""
    select count(e) > 0 from EntreeSorties e
    where e.vehicules.vehicule_id = :vehiculeId
      and e.dateHeureSortie is null
""")
    boolean existsPresentByVehiculeId(@Param("vehiculeId") Long vehiculeId);
    @Query("""
    select count(e) > 0 from EntreeSorties e
    where e.places.place_id = :placeId
      and e.dateHeureSortie is null
""")
    boolean existsPresentByPlaceId(@Param("placeId") Long placeId);
    @Query("""
    select e from EntreeSorties e
    where e.vehicules.immatriculation = :immat and e.dateHeureSortie is null
""")
    Optional<EntreeSorties> findOuverteByImmatriculation(@Param("immat") String immat);

    @Query("select e from EntreeSorties e where e.statut = :statut and e.dateHeureSortie is null order by e.dateHeureEntree desc")
    List<EntreeSorties> findSessionsActives(@Param("statut") StatutSortie statut);

    @Query("""
    select e from EntreeSorties e
    where e.vehicules.immatriculation = :immat
    order by e.dateHeureEntree desc
""")
    List<EntreeSorties> findHistorique(@Param("immat") String immat);
    @Query("""
    select e from EntreeSorties e
    where e.places.type_place = :type
      and e.dateHeureSortie is null
      and e.placeRepli = false
      and e.occupationSignalee = false
""")
    List<EntreeSorties> findOccupationsNonSignalees(@Param("type") TypePlace type);
    long countByStatut(StatutSortie statut);
    @Query("select e.dateHeureEntree from EntreeSorties e where e.dateHeureEntree >= :debut and e.dateHeureEntree < :fin")
    List<LocalDateTime> findDatesEntreeEntre(@Param("debut") LocalDateTime debut,
                                             @Param("fin") LocalDateTime fin);
    @Query("""
    select e.dateHeureEntree, e.dateHeureSortie from EntreeSorties e
    where e.statut = :statut
      and e.dateHeureSortie >= :debut
      and e.dateHeureSortie < :fin
""")
    List<Object[]> findDureesTerminees(@Param("statut") StatutSortie statut,
                                       @Param("debut") LocalDateTime debut,
                                       @Param("fin") LocalDateTime fin);
}
