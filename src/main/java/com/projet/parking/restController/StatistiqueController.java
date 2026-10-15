package com.projet.parking.restController;

import com.projet.parking.dto.TableauDeBordDTO;
import com.projet.parking.dto.ZoneStatDTO;
import com.projet.parking.enumeration.TypePaiement;
import com.projet.parking.service.StatistiqueService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatistiqueController {

    private final StatistiqueService statistiqueService;

    public StatistiqueController(StatistiqueService statistiqueService) {
        this.statistiqueService = statistiqueService;
    }

    @GetMapping("/occupation")
    public Map<String, Object> occupation() {
        return Map.of("tauxOccupation", statistiqueService.getTauxOccupation(),
                "parType", statistiqueService.getTauxOccupationParType(),
                "sessionsActives", statistiqueService.getSessionsActives());
    }


    @GetMapping("/zones")
    public List<ZoneStatDTO> parZone() {
        return statistiqueService.getTauxOccupationParZone();
    }

    @GetMapping("/chiffre-affaires")
    public Map<String, BigDecimal> chiffreAffaires(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return Map.of("chiffreAffaires", statistiqueService.getChiffreAffaires(debut, fin));
    }

    @GetMapping("/chiffre-affaires/par-type")
    public Map<TypePaiement, BigDecimal> chiffreAffairesParType(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return statistiqueService.getChiffreAffairesParType(debut, fin);
    }

    @GetMapping("/chiffre-affaires/par-jour")
    public Map<LocalDate, BigDecimal> chiffreAffairesParJour(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return statistiqueService.getChiffreAffairesParJour(debut, fin);
    }

    @GetMapping("/frequentation")
    public Map<String, Long> frequentation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return Map.of("entrees", statistiqueService.getFrequentation(debut, fin));
    }

    @GetMapping("/frequentation/par-jour")
    public Map<LocalDate, Long> frequentationParJour(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return statistiqueService.getFrequentationParJour(debut, fin);
    }

    @GetMapping("/frequentation/par-heure")
    public Map<Integer, Long> frequentationParHeure(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return statistiqueService.getFrequentationParHeure(debut, fin);
    }

    @GetMapping("/duree-moyenne")
    public Map<String, Long> dureeMoyenne(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return Map.of("minutes", statistiqueService.getDureeMoyenneStationnement(debut, fin).toMinutes());
    }

    @GetMapping("/tableau-de-bord")
    public TableauDeBordDTO tableauDeBord(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return statistiqueService.getTableauDeBord(debut, fin);
    }
}