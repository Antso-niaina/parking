package com.projet.parking.restController;

import com.projet.parking.entity.Clients;
import com.projet.parking.service.PlacePrioritaireService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/justificatifs")
public class JustificatifController {

    public record DeposerJustificatifRequest(@NotBlank String numeroCarte, LocalDate dateExpiration) {}

    private final PlacePrioritaireService placePrioritaireService;

    public JustificatifController(PlacePrioritaireService placePrioritaireService) {
        this.placePrioritaireService = placePrioritaireService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> deposer(@AuthenticationPrincipal Clients moi,
                                     @Valid @RequestBody DeposerJustificatifRequest req) {
        Long id = placePrioritaireService.enregistrerJustificatif(moi.getClient_id(), req.numeroCarte(),
                req.dateExpiration());
        return Map.of("justificatif_id", id);
    }

    @PostMapping("/{id}/valider")   // ADMIN (SecurityConfig)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void valider(@PathVariable Long id) {
        placePrioritaireService.validerJustificatif(id);
    }

    @PostMapping("/{id}/refuser")   // ADMIN (SecurityConfig)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void refuser(@PathVariable Long id) {
        placePrioritaireService.refuserJustificatif(id);
    }
}
