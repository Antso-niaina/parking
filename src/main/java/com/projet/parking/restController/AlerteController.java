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
@RequestMapping("/api/alertes")
public class AlerteController {

    private final AlerteService alerteService;

    public AlerteController(AlerteService alerteService) {
        this.alerteService = alerteService;
    }

    @GetMapping("/moi")
    public List<AlerteDTO> mesAlertes(@AuthenticationPrincipal Clients moi) {
        return alerteService.getAlertesUtilisateur(moi.getClient_id());
    }

    @GetMapping("/admin")   // ADMIN (SecurityConfig)
    public List<AlerteDTO> alertesAdmin() {
        return alerteService.getAlertesAdministrateurs();
    }

    @PostMapping("/{id}/lue")
    @PreAuthorize("hasRole('ADMIN') or @securite.proprietaireAlerte(#id, authentication)")
    public AlerteDTO marquerLue(@PathVariable Long id) {
        return alerteService.marquerLue(id);
    }
}
