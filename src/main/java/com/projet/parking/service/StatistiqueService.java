package com.projet.parking.service;

import com.projet.parking.dto.TableauDeBordDTO;
import com.projet.parking.dto.ZoneStatDTO;
import com.projet.parking.entity.Paiement;
import com.projet.parking.entity.Zones;
import com.projet.parking.enumeration.StatutPaiement;
import com.projet.parking.enumeration.StatutPlace;
import com.projet.parking.enumeration.StatutSortie;
import com.projet.parking.enumeration.TypePaiement;
import com.projet.parking.enumeration.TypePlace;
import com.projet.parking.exception.OperationNonAutoriseeException;
import com.projet.parking.repository.EntreeSortiesRepository;
import com.projet.parking.repository.PaiementRepository;
import com.projet.parking.repository.PlacesRepository;
import com.projet.parking.repository.ZonesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;


@Service
@Transactional(readOnly = true)
public class StatistiqueService {


    private static final List<StatutPaiement> ENCAISSES = List.of(StatutPaiement.PAYEE, StatutPaiement.REMBOURSEE);

    private final PlacesService placesService;
    private final PlacesRepository placesRepository;
    private final PaiementRepository paiementRepository;
    private final EntreeSortiesRepository entreeSortiesRepository;
    private final ZonesRepository zonesRepository;

    public StatistiqueService(PlacesService placesService,
                              PlacesRepository placesRepository,
                              PaiementRepository paiementRepository,
                              EntreeSortiesRepository entreeSortiesRepository,
                              ZonesRepository zonesRepository) {
        this.placesService = placesService;
        this.placesRepository = placesRepository;
        this.paiementRepository = paiementRepository;
        this.entreeSortiesRepository = entreeSortiesRepository;
        this.zonesRepository = zonesRepository;
    }




    public double getTauxOccupation() {
        return placesService.getTauxOccupation();
    }

    public Map<TypePlace, Double> getTauxOccupationParType() {
        Map<TypePlace, Long> occupees = new EnumMap<>(TypePlace.class);
        for (Object[] ligne : placesRepository.compterParTypeEtStatut(StatutPlace.OCCUPEE)) {
            occupees.put((TypePlace) ligne[0], (Long) ligne[1]);
        }
        Map<TypePlace, Double> resultat = new EnumMap<>(TypePlace.class);
        for (TypePlace type : TypePlace.values()) {
            long exploitables = placesRepository.compterParTypeSaufStatut(type, StatutPlace.HORS_SERVICE);
            resultat.put(type, exploitables == 0
                    ? 0.0
                    : Math.round(occupees.getOrDefault(type, 0L) * 10000.0 / exploitables) / 100.0);
        }
        return resultat;
    }






    public List<ZoneStatDTO> getTauxOccupationParZone() {
        Map<Long, Map<StatutPlace, Long>> parZone = new HashMap<>();
        for (Object[] ligne : placesRepository.compterParZoneEtStatut()) {
            parZone.computeIfAbsent((Long) ligne[0], k -> new EnumMap<>(StatutPlace.class))
                    .put((StatutPlace) ligne[1], (Long) ligne[2]);
        }

        List<ZoneStatDTO> resultat = new ArrayList<>();
        for (Zones zone : zonesRepository.findAllByOrderByNomAsc()) {
            Map<StatutPlace, Long> statuts = parZone.getOrDefault(zone.getZone_id(), Map.of());
            long nbPlaces = statuts.values().stream().mapToLong(Long::longValue).sum();
            long occupees = statuts.getOrDefault(StatutPlace.OCCUPEE, 0L);
            long horsService = statuts.getOrDefault(StatutPlace.HORS_SERVICE, 0L);
            long exploitables = nbPlaces - horsService;

            double tauxOccupation = exploitables == 0 ? 0.0 : Math.round(occupees * 10000.0 / exploitables) / 100.0;
            double tauxRemplissage = Math.round(nbPlaces * 10000.0 / zone.getCapaciteTotal()) / 100.0;
            resultat.add(new ZoneStatDTO(zone.getZone_id(), zone.getNom(), zone.getCapaciteTotal(),
                    nbPlaces, occupees, horsService, tauxOccupation, tauxRemplissage));
        }
        return resultat;
    }

    public long getSessionsActives() {
        return entreeSortiesRepository.countByStatut(StatutSortie.EN_COURS);
    }




    public BigDecimal getChiffreAffaires(LocalDateTime debut, LocalDateTime fin) {
        verifierPeriode(debut, fin);
        return paiementRepository.findEncaissesEntre(ENCAISSES, debut, fin).stream()
                .map(this::net)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    public Map<TypePaiement, BigDecimal> getChiffreAffairesParType(LocalDateTime debut, LocalDateTime fin) {
        verifierPeriode(debut, fin);
        Map<TypePaiement, BigDecimal> resultat = new EnumMap<>(TypePaiement.class);
        for (TypePaiement type : TypePaiement.values()) {
            resultat.put(type, BigDecimal.ZERO);
        }
        for (Paiement p : paiementRepository.findEncaissesEntre(ENCAISSES, debut, fin)) {
            resultat.merge(p.getTypePaiement(), net(p), BigDecimal::add);
        }
        return resultat;
    }


    public Map<LocalDate, BigDecimal> getChiffreAffairesParJour(LocalDateTime debut, LocalDateTime fin) {
        verifierPeriode(debut, fin);
        Map<LocalDate, BigDecimal> resultat = new TreeMap<>();
        for (Paiement p : paiementRepository.findEncaissesEntre(ENCAISSES, debut, fin)) {
            resultat.merge(p.getDateValidation().toLocalDate(), net(p), BigDecimal::add);
        }
        return resultat;
    }




    public long getFrequentation(LocalDateTime debut, LocalDateTime fin) {
        verifierPeriode(debut, fin);
        return entreeSortiesRepository.findDatesEntreeEntre(debut, fin).size();
    }

    public Map<LocalDate, Long> getFrequentationParJour(LocalDateTime debut, LocalDateTime fin) {
        verifierPeriode(debut, fin);
        return entreeSortiesRepository.findDatesEntreeEntre(debut, fin).stream()
                .collect(Collectors.groupingBy(LocalDateTime::toLocalDate, TreeMap::new, Collectors.counting()));
    }


    public Map<Integer, Long> getFrequentationParHeure(LocalDateTime debut, LocalDateTime fin) {
        verifierPeriode(debut, fin);
        Map<Integer, Long> resultat = new TreeMap<>();
        for (int h = 0; h < 24; h++) {
            resultat.put(h, 0L);
        }
        for (LocalDateTime entree : entreeSortiesRepository.findDatesEntreeEntre(debut, fin)) {
            resultat.merge(entree.getHour(), 1L, Long::sum);
        }
        return resultat;
    }




    public Duration getDureeMoyenneStationnement(LocalDateTime debut, LocalDateTime fin) {
        verifierPeriode(debut, fin);
        List<Object[]> sessions = entreeSortiesRepository.findDureesTerminees(StatutSortie.TERMINE, debut, fin);
        if (sessions.isEmpty()) {
            return Duration.ZERO;
        }
        long totalSecondes = 0;
        for (Object[] s : sessions) {
            totalSecondes += Duration.between((LocalDateTime) s[0], (LocalDateTime) s[1]).getSeconds();
        }
        return Duration.ofSeconds(totalSecondes / sessions.size());
    }



    public TableauDeBordDTO getTableauDeBord(LocalDateTime debut, LocalDateTime fin) {
        verifierPeriode(debut, fin);
        return new TableauDeBordDTO(
                getTauxOccupation(),
                getTauxOccupationParType(),
                getSessionsActives(),
                getChiffreAffaires(debut, fin),
                getChiffreAffairesParType(debut, fin),
                getFrequentation(debut, fin),
                getDureeMoyenneStationnement(debut, fin).toMinutes());
    }



    private BigDecimal net(Paiement p) {
        BigDecimal rembourse = p.getMontantRembourse() != null ? p.getMontantRembourse() : BigDecimal.ZERO;
        return p.getMontant().subtract(rembourse);
    }

    private void verifierPeriode(LocalDateTime debut, LocalDateTime fin) {
        if (debut == null || fin == null) {
            throw new OperationNonAutoriseeException("Début et fin de période sont obligatoires");
        }
        if (!fin.isAfter(debut)) {
            throw new OperationNonAutoriseeException("La fin doit être après le début");
        }
    }
}