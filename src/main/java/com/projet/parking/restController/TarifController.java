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
@RequestMapping("/api/tarifs")
public class TarifController {

    private final TarifsService tarifsService;

    public TarifController(TarifsService tarifsService) {
        this.tarifsService = tarifsService;
    }

    @GetMapping("/actuel")   // tout utilisateur connecté (SecurityConfig)
    public TarifsDTO actuel() {
        return TarifsMapper.toDto(tarifsService.getTarifActuel());
    }

    @GetMapping("/historique")   // ADMIN
    public List<TarifsDTO> historique() {
        return tarifsService.getHistoriqueTarifs().stream().map(TarifsMapper::toDto).toList();
    }

    @PostMapping   // ADMIN : nouvelle grille, les anciennes sont conservées
    @ResponseStatus(HttpStatus.CREATED)
    public TarifsDTO definir(@AuthenticationPrincipal Clients moi, @Valid @RequestBody TarifsDTO dto) {
        return TarifsMapper.toDto(tarifsService.definirTarif(moi.getClient_id(), TarifsMapper.toEntity(dto)));
    }
}
