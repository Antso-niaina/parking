package com.projet.parking.restController;

import com.projet.parking.dto.FactureDTO;
import com.projet.parking.dto.PaiementDTO;
import com.projet.parking.dto.RecuDTO;
import com.projet.parking.entity.Clients;
import com.projet.parking.enumeration.TypePaiement;
import com.projet.parking.service.FactureService;
import com.projet.parking.service.PaiementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/paiements")
public class PaiementController {

    public record CreerPaiementRequest(@Positive BigDecimal montant,
                                       @NotNull TypePaiement typePaiement, @NotNull Long cibleId) {}

    private final PaiementService paiementService;
    private final FactureService factureService;

    public PaiementController(PaiementService paiementService, FactureService factureService) {
        this.paiementService = paiementService;
        this.factureService = factureService;
    }

    @PostMapping
    @PreAuthorize("@securite.peutCreerPaiement(#req.typePaiement(), #req.cibleId(), authentication)")
    @ResponseStatus(HttpStatus.CREATED)
    public PaiementDTO creer(@Valid @RequestBody CreerPaiementRequest req) {
        return paiementService.creerPaiement(req.montant(), req.typePaiement(), req.cibleId());
    }


    @GetMapping("/mes")
    public List<PaiementDTO> mesPaiements(@AuthenticationPrincipal Clients moi) {
        return paiementService.getPaiementsParUtilisateur(moi.getClient_id());
    }


    @GetMapping("/utilisateur/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @securite.estMoi(#userId, authentication)")
    public List<PaiementDTO> parUtilisateur(@PathVariable Long userId) {
        return paiementService.getPaiementsParUtilisateur(userId);
    }

    @PostMapping("/{id}/valider")
    public PaiementDTO valider(@PathVariable Long id) {
        return paiementService.validerPaiement(id);
    }

    @PostMapping("/{id}/echec")
    public PaiementDTO echec(@PathVariable Long id) {
        return paiementService.marquerEchoue(id);
    }

    @PostMapping("/{id}/rembourser")
    public PaiementDTO rembourser(@PathVariable Long id, @RequestParam(required = false) BigDecimal montant) {
        return paiementService.rembourser(id, montant);
    }

    @GetMapping("/{id}/recu")
    @PreAuthorize("hasAnyRole('ADMIN','BORNE') or @securite.proprietairePaiement(#id, authentication)")
    public RecuDTO recu(@PathVariable Long id) {
        return factureService.genererRecu(id);
    }

    @GetMapping("/{id}/facture")
    @PreAuthorize("hasAnyRole('ADMIN','BORNE') or @securite.proprietairePaiement(#id, authentication)")
    public FactureDTO facture(@PathVariable Long id) {
        return factureService.genererFacture(id);
    }
}
