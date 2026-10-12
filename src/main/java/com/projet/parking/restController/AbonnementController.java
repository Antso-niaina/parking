package com.projet.parking.restController;

import com.projet.parking.dto.AbonnementsDTO;
import com.projet.parking.dto.RemboursementDTO;
import com.projet.parking.entity.Clients;
import com.projet.parking.enumeration.FormuleAbonnement;
import com.projet.parking.enumeration.TypeAbonnements;
import com.projet.parking.service.AbonnementService;
import com.projet.parking.service.PaiementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/abonnements")
public class AbonnementController {

    public record SouscrireRequest(@NotEmpty List<Long> vehiculeIds, @NotNull FormuleAbonnement formule,
                                   @NotNull LocalDateTime dateDebut, @NotNull TypeAbonnements duree,
                                   boolean renouvellementAuto) {}

    private final AbonnementService abonnementService;
    private final PaiementService paiementService;                       // NOUVEAU

    public AbonnementController(AbonnementService abonnementService, PaiementService paiementService) {
        this.abonnementService = abonnementService;
        this.paiementService = paiementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AbonnementsDTO souscrire(@AuthenticationPrincipal Clients moi, @Valid @RequestBody SouscrireRequest req) {
        return abonnementService.souscrire(moi.getClient_id(), req.vehiculeIds(), req.formule(),
                req.dateDebut(), req.duree(), req.renouvellementAuto());
    }

    // NOUVEAU
    @GetMapping("/mes")
    public List<AbonnementsDTO> mesAbonnements(@AuthenticationPrincipal Clients moi) {
        return abonnementService.getAbonnementsParUtilisateur(moi.getClient_id());
    }

    // NOUVEAU
    @GetMapping("/utilisateur/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @securite.estMoi(#userId, authentication)")
    public List<AbonnementsDTO> parUtilisateur(@PathVariable Long userId) {
        return abonnementService.getAbonnementsParUtilisateur(userId);
    }

    @PostMapping("/{id}/renouveler")
    @PreAuthorize("hasRole('ADMIN') or @securite.proprietaireAbonnement(#id, authentication)")
    public AbonnementsDTO renouveler(@PathVariable Long id) {
        return abonnementService.renouveler(id);
    }

    @PostMapping("/{id}/suspendre")
    @PreAuthorize("hasRole('ADMIN')")
    public AbonnementsDTO suspendre(@PathVariable Long id) {
        return abonnementService.suspendre(id);
    }

    // MODIFIÉ : résilie ET enregistre le remboursement
    @PostMapping("/{id}/resilier")
    @PreAuthorize("hasRole('ADMIN') or @securite.proprietaireAbonnement(#id, authentication)")
    public RemboursementDTO resilier(@PathVariable Long id) {
        return paiementService.resilierAbonnementEtRembourser(id);
    }
}