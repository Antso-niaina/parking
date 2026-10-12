package com.projet.parking.restController;

import com.projet.parking.dto.PlacesDTO;
import com.projet.parking.dto.ZonesDTO;
import com.projet.parking.service.ZonesService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/zones")
public class ZoneController {

    private final ZonesService zonesService;

    public ZoneController(ZonesService zonesService) {
        this.zonesService = zonesService;
    }

    @GetMapping
    public List<ZonesDTO> lister() {
        return zonesService.listerZones();
    }

    @GetMapping("/{id}")
    public ZonesDTO consulter(@PathVariable Long id) {
        return zonesService.getZone(id);
    }

    @GetMapping("/{id}/places")
    public List<PlacesDTO> places(@PathVariable Long id) {
        return zonesService.getPlacesDeLaZone(id);
    }

    @GetMapping("/places-sans-zone")
    @PreAuthorize("hasRole('ADMIN')")
    public List<PlacesDTO> placesSansZone() {
        return zonesService.getPlacesSansZone();
    }

    // les trois suivants : ADMIN (SecurityConfig)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ZonesDTO creer(@Valid @RequestBody ZonesDTO dto) {
        return zonesService.creerZone(dto);
    }

    @PutMapping("/{id}")
    public ZonesDTO modifier(@PathVariable Long id, @Valid @RequestBody ZonesDTO dto) {
        return zonesService.modifierZone(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        zonesService.supprimerZone(id);
    }
}
