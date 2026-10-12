package com.projet.parking.restController;

import com.projet.parking.dto.PlacesDTO;
import com.projet.parking.enumeration.StatutPlace;
import com.projet.parking.enumeration.TypePlace;
import com.projet.parking.enumeration.TypeVehicule;
import com.projet.parking.mapper.PlacesMapper;
import com.projet.parking.service.PlacesService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/places")
public class PlaceController {

    // MODIFIÉ : la zone est maintenant obligatoire
    public record CreerPlaceRequest(@NotBlank String numero, int niveau,
                                    @NotNull TypePlace type, @NotNull Long zoneId) {}

    private final PlacesService placesService;

    public PlaceController(PlacesService placesService) {
        this.placesService = placesService;
    }

    @GetMapping("/disponibles")
    public PlacesDTO trouverDisponible(
            @RequestParam TypeVehicule typeVehicule,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return PlacesMapper.toDto(placesService.trouverPlaceDisponible(typeVehicule, debut, fin));
    }

    @GetMapping("/disponibles-par-type")
    public Map<TypePlace, Long> disponiblesParType() {
        return placesService.getPlacesDisponiblesParType();
    }

    @GetMapping("/taux-occupation")
    public Map<String, Double> tauxOccupation() {
        return Map.of("tauxOccupation", placesService.getTauxOccupation());
    }

    // les quatre suivants : ADMIN (SecurityConfig)

    // MODIFIÉ : transmet la zone à creerPlace
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlacesDTO creer(@Valid @RequestBody CreerPlaceRequest req) {
        return PlacesMapper.toDto(
                placesService.creerPlace(req.numero(), req.niveau(), req.type(), req.zoneId()));
    }

    // NOUVEAU : rattache une place existante à une zone, ou la déplace vers une autre
    @PatchMapping("/{id}/zone")
    public PlacesDTO affecterZone(@PathVariable Long id, @RequestParam Long zoneId) {
        return PlacesMapper.toDto(placesService.affecterZone(id, zoneId));
    }

    @PatchMapping("/{id}/statut")
    public PlacesDTO changerStatut(@PathVariable Long id, @RequestParam StatutPlace statut) {
        return PlacesMapper.toDto(placesService.changerStatut(id, statut));
    }

    @PostMapping("/{id}/maintenance")
    public PlacesDTO mettreEnMaintenance(@PathVariable Long id) {
        return PlacesMapper.toDto(placesService.mettreEnMaintenance(id));
    }
}