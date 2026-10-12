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
@RequestMapping("/api/acces")   // BORNE ou ADMIN (SecurityConfig)
public class AccesController {

    public record EntreeRequest(@NotBlank String plaque, TypeVehicule typeVehicule) {}
    public record PlaqueRequest(@NotBlank String plaque) {}
    public record AnomalieRequest(@NotNull TypeAnomalie type, @NotBlank String details) {}

    private final AccesService accesService;

    public AccesController(AccesService accesService) {
        this.accesService = accesService;
    }

    @PostMapping("/entree")
    @ResponseStatus(HttpStatus.CREATED)
    public EntreeSortiesDTO entree(@Valid @RequestBody EntreeRequest req) {
        return req.typeVehicule() != null
                ? accesService.enregistrerEntree(req.plaque(), req.typeVehicule())
                : accesService.enregistrerEntree(req.plaque());
    }

    @GetMapping("/montant-du")
    public Map<String, BigDecimal> montantDu(@RequestParam String plaque,
                                             @RequestParam(defaultValue = "false") boolean ticketPerdu) {
        return Map.of("montantDu", accesService.calculerMontantDu(plaque, ticketPerdu));
    }

    @PostMapping("/sortie")
    public EntreeSortiesDTO sortie(@Valid @RequestBody PlaqueRequest req) {
        return accesService.enregistrerSortie(req.plaque());
    }

    @PostMapping("/ticket-perdu")
    public EntreeSortiesDTO ticketPerdu(@Valid @RequestBody PlaqueRequest req) {
        return accesService.gererTicketPerdu(req.plaque());
    }

    @PostMapping("/anomalies")
    @ResponseStatus(HttpStatus.CREATED)
    public void signalerAnomalie(@Valid @RequestBody AnomalieRequest req) {
        accesService.signalerAnomalie(req.type(), req.details());
    }

    @GetMapping("/sessions-actives")
    public List<EntreeSortiesDTO> sessionsActives() {
        return accesService.getSessionsActives();
    }

    @GetMapping("/historique/{plaque}")
    public List<EntreeSortiesDTO> historique(@PathVariable String plaque) {
        return accesService.getHistorique(plaque);
    }
}
