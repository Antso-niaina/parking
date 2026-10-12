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
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securite.estMoi(#id, authentication)")
    public ClientsDTO recuperer(@PathVariable Long id) {
        return clientService.recuperer(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securite.estMoi(#id, authentication)")
    public ClientsDTO modifier(@PathVariable Long id, @Valid @RequestBody ClientsDTO dto) {
        return clientService.modifier(id, dto);
    }

    @PostMapping("/{id}/suspendre")   // ADMIN (SecurityConfig)
    public ClientsDTO suspendre(@PathVariable Long id) {
        return clientService.suspendreUtilisateur(id);
    }

    @PostMapping("/{id}/reactiver")   // ADMIN (SecurityConfig)
    public ClientsDTO reactiver(@PathVariable Long id) {
        return clientService.reactiverUtilisateur(id);
    }

    @DeleteMapping("/{id}")           // ADMIN (SecurityConfig)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        clientService.supprimerUtilisateur(id);
    }
}