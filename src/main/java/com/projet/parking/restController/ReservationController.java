package com.projet.parking.restController;

import com.projet.parking.dto.RemboursementDTO;
import com.projet.parking.dto.ReservationsDTO;
import com.projet.parking.entity.Clients;
import com.projet.parking.service.PaiementService;
import com.projet.parking.service.ReservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    public record CreerReservationRequest(@NotNull Long vehiculeId, @NotNull Long placeId,
                                          @NotNull LocalDateTime debut, @NotNull LocalDateTime fin) {}

    public record ModifierReservationRequest(LocalDateTime debut, LocalDateTime fin, Long placeId) {}

    private final ReservationService reservationService;
    private final PaiementService paiementService;                       // NOUVEAU

    public ReservationController(ReservationService reservationService, PaiementService paiementService) {
        this.reservationService = reservationService;
        this.paiementService = paiementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationsDTO creer(@AuthenticationPrincipal Clients moi,
                                 @Valid @RequestBody CreerReservationRequest req) {
        return reservationService.creerReservation(moi.getClient_id(), req.vehiculeId(), req.placeId(),
                req.debut(), req.fin());
    }

    // NOUVEAU
    @GetMapping("/mes")
    public List<ReservationsDTO> mesReservations(@AuthenticationPrincipal Clients moi) {
        return reservationService.getReservationsParUtilisateur(moi.getClient_id());
    }

    // NOUVEAU
    @GetMapping("/utilisateur/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @securite.estMoi(#userId, authentication)")
    public List<ReservationsDTO> parUtilisateur(@PathVariable Long userId) {
        return reservationService.getReservationsParUtilisateur(userId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securite.proprietaireReservation(#id, authentication)")
    public ReservationsDTO modifier(@PathVariable Long id, @RequestBody ModifierReservationRequest req) {
        return reservationService.modifierReservation(id, req.debut(), req.fin(), req.placeId());
    }

    // MODIFIÉ : annule ET enregistre le remboursement
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securite.proprietaireReservation(#id, authentication)")
    public RemboursementDTO annuler(@PathVariable Long id) {
        return paiementService.annulerReservationEtRembourser(id);
    }
}