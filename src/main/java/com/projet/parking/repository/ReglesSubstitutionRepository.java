package com.projet.parking.repository;

import com.projet.parking.entity.ReglesSubstitution;
import com.projet.parking.enumeration.TypePlace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReglesSubstitutionRepository extends JpaRepository<ReglesSubstitution,Long> {
    List<ReglesSubstitution> findByTypePlaceDemandeOrderByOrdrePrioriteAsc(TypePlace typePlaceDemande);
}
