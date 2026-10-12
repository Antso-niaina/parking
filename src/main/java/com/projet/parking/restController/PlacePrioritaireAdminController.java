package com.projet.parking.restController;

import com.projet.parking.service.PlacePrioritaireService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/places-prioritaires")
public class PlacePrioritaireAdminController {

    private final PlacePrioritaireService placePrioritaireService;

    public PlacePrioritaireAdminController(PlacePrioritaireService placePrioritaireService) {
        this.placePrioritaireService = placePrioritaireService;
    }

    @PostMapping("/quota")
    public Map<String, Integer> garantirQuota() {
        return Map.of("conversions", placePrioritaireService.garantirQuotaMinimum());
    }

    @PostMapping("/detection")
    public Map<String, Integer> detecterOccupations() {
        return Map.of("alertes", placePrioritaireService.detecterOccupationIllegitime());
    }
}