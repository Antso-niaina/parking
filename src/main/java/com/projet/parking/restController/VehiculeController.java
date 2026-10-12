package com.projet.parking.restController;

import com.projet.parking.dto.*;
import com.projet.parking.entity.Clients;
import com.projet.parking.enumeration.*;
import com.projet.parking.mapper.*;
import com.projet.parking.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicules")
public class VehiculeController {

    private final VehiculesService vehiculesService;

    public VehiculeController(VehiculesService vehiculesService) {
        this.vehiculesService = vehiculesService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehiculesDTO enregistrer(@AuthenticationPrincipal Clients moi, @Valid @RequestBody VehiculesDTO dto) {
        if (moi.getType_client() != ClientsEnum.ADMIN) {
            dto.setClient_id(moi.getClient_id());
        }
        return vehiculesService.enregistrerVehicule(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securite.proprietaireVehicule(#id, authentication)")
    public VehiculesDTO modifier(@AuthenticationPrincipal Clients moi, @PathVariable Long id,
                                 @Valid @RequestBody VehiculesDTO dto) {
        if (moi.getType_client() != ClientsEnum.ADMIN) {
            dto.setClient_id(moi.getClient_id());
        }
        return vehiculesService.modifierVehicule(id, dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securite.proprietaireVehicule(#id, authentication)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        vehiculesService.supprimerVehicule(id);
    }

    @GetMapping("/mes")
    public List<VehiculesDTO> mesVehicules(@AuthenticationPrincipal Clients moi) {
        return vehiculesService.getVehiculesParUtilisateur(moi.getClient_id());
    }

    @GetMapping("/utilisateur/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @securite.estMoi(#userId, authentication)")
    public List<VehiculesDTO> parUtilisateur(@PathVariable Long userId) {
        return vehiculesService.getVehiculesParUtilisateur(userId);
    }

    @GetMapping("/present")
    @PreAuthorize("hasAnyRole('ADMIN','BORNE')")
    public Map<String, Boolean> estPresent(@RequestParam String plaque) {
        return Map.of("present", vehiculesService.estPresentDansParking(plaque));
    }
}