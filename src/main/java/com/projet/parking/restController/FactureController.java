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
@RequestMapping("/api/factures")
public class FactureController {

    private final FactureService factureService;

    public FactureController(FactureService factureService) {
        this.factureService = factureService;
    }

    @GetMapping("/impayes")
    public List<FactureDTO> mesImpayes(@AuthenticationPrincipal Clients moi) {
        return factureService.getImpayes(moi.getClient_id());
    }

    @GetMapping("/impayes/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<FactureDTO> impayesDe(@PathVariable Long userId) {
        return factureService.getImpayes(userId);
    }
}
