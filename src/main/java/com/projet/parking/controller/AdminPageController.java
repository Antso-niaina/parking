package com.projet.parking.controller;

import com.projet.parking.config.SecuriteService;
import com.projet.parking.dto.*;
import com.projet.parking.entity.*;
import com.projet.parking.enumeration.*;
import com.projet.parking.exception.*;
import com.projet.parking.mapper.*;
import com.projet.parking.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminPageController {

    @Data public static class PlaceForm {
        @NotBlank private String numero;
        private int niveau;
        @NotNull private TypePlace type;
        @NotNull private Long zoneId;
    }

    private final StatistiqueService statistiqueService;
    private final PlacesService placesService;
    private final ZonesService zonesService;
    private final TarifsService tarifsService;
    private final PlacePrioritaireService placePrioritaireService;
    private final PaiementService paiementService;
    private final AlerteService alerteService;

    public AdminPageController(StatistiqueService statistiqueService, PlacesService placesService,
                               ZonesService zonesService, TarifsService tarifsService,
                               PlacePrioritaireService placePrioritaireService,
                               PaiementService paiementService, AlerteService alerteService) {
        this.statistiqueService = statistiqueService;
        this.placesService = placesService;
        this.zonesService = zonesService;
        this.tarifsService = tarifsService;
        this.placePrioritaireService = placePrioritaireService;
        this.paiementService = paiementService;
        this.alerteService = alerteService;
    }

    // ----------------------------------------------------------------- tableau de bord

    /** Période par défaut : les 30 derniers jours, jour de fin inclus. */
    @GetMapping
    public String tableauDeBord(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
                                Model model) {
        LocalDate jourFin = fin != null ? fin : LocalDate.now();
        LocalDate jourDebut = debut != null ? debut : jourFin.minusDays(29);
        LocalDateTime d = jourDebut.atStartOfDay();
        LocalDateTime f = jourFin.plusDays(1).atStartOfDay();

        model.addAttribute("debut", jourDebut);
        model.addAttribute("fin", jourFin);
        model.addAttribute("tableau", statistiqueService.getTableauDeBord(d, f));
        model.addAttribute("zones", statistiqueService.getTauxOccupationParZone());
        model.addAttribute("caParJour", statistiqueService.getChiffreAffairesParJour(d, f));
        model.addAttribute("frequentationParHeure", statistiqueService.getFrequentationParHeure(d, f));
        model.addAttribute("alertes", alerteService.getAlertesAdministrateurs());
        return "admin/tableau-de-bord";
    }

    // ----------------------------------------------------------------- places

    @GetMapping("/places")
    public String places(Model model) {
        model.addAttribute("places", placesService.listerPlaces());
        model.addAttribute("zones", zonesService.listerZones());
        model.addAttribute("form", new PlaceForm());
        model.addAttribute("types", TypePlace.values());
        model.addAttribute("statuts", StatutPlace.values());
        return "admin/places";
    }

    @PostMapping("/places")
    public String creerPlace(@Valid @ModelAttribute("form") PlaceForm form, BindingResult result,
                             RedirectAttributes ra) {
        if (result.hasErrors()) {
            return invalide(result, ra, "/admin/places");
        }
        placesService.creerPlace(form.getNumero(), form.getNiveau(), form.getType(), form.getZoneId());
        ra.addFlashAttribute("message", "Place créée");
        return "redirect:/admin/places";
    }

    @PostMapping("/places/{id}/statut")
    public String changerStatut(@PathVariable Long id, @RequestParam StatutPlace statut, RedirectAttributes ra) {
        placesService.changerStatut(id, statut);
        ra.addFlashAttribute("message", "Statut modifié");
        return "redirect:/admin/places";
    }

    @PostMapping("/places/{id}/maintenance")
    public String maintenance(@PathVariable Long id, RedirectAttributes ra) {
        placesService.mettreEnMaintenance(id);
        ra.addFlashAttribute("message", "Place mise hors service");
        return "redirect:/admin/places";
    }

    @PostMapping("/places/{id}/zone")
    public String affecterZone(@PathVariable Long id, @RequestParam Long zoneId, RedirectAttributes ra) {
        placesService.affecterZone(id, zoneId);
        ra.addFlashAttribute("message", "Place rattachée à la zone");
        return "redirect:/admin/places";
    }

    // ----------------------------------------------------------------- zones

    @GetMapping("/zones")
    public String zones(Model model) {
        model.addAttribute("zones", zonesService.listerZones());
        model.addAttribute("zone", new ZonesDTO());
        return "admin/zones";
    }

    @PostMapping("/zones")
    public String creerZone(@Valid @ModelAttribute("zone") ZonesDTO dto, BindingResult result,
                            RedirectAttributes ra) {
        if (result.hasErrors()) {
            return invalide(result, ra, "/admin/zones");
        }
        zonesService.creerZone(dto);
        ra.addFlashAttribute("message", "Zone créée");
        return "redirect:/admin/zones";
    }

    @PostMapping("/zones/{id}")
    public String modifierZone(@PathVariable Long id, @Valid @ModelAttribute("zone") ZonesDTO dto,
                               BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return invalide(result, ra, "/admin/zones");
        }
        zonesService.modifierZone(id, dto);
        ra.addFlashAttribute("message", "Zone modifiée");
        return "redirect:/admin/zones";
    }

    @PostMapping("/zones/{id}/supprimer")
    public String supprimerZone(@PathVariable Long id, RedirectAttributes ra) {
        zonesService.supprimerZone(id);
        ra.addFlashAttribute("message", "Zone supprimée");
        return "redirect:/admin/zones";
    }

    // ----------------------------------------------------------------- tarifs

    @GetMapping("/tarifs")
    public String tarifs(Model model) {
        TarifsDTO actuel;
        try {
            actuel = TarifsMapper.toDto(tarifsService.getTarifActuel());
        } catch (ResourceNotFoundException e) {
            actuel = new TarifsDTO();   // aucune grille pour l'instant
        }
        model.addAttribute("tarif", actuel);
        model.addAttribute("historique",
                tarifsService.getHistoriqueTarifs().stream().map(TarifsMapper::toDto).toList());
        return "admin/tarifs";
    }

    @PostMapping("/tarifs")
    public String definirTarif(@AuthenticationPrincipal Clients moi,
                               @Valid @ModelAttribute("tarif") TarifsDTO dto, BindingResult result,
                               RedirectAttributes ra) {
        if (result.hasErrors()) {
            return invalide(result, ra, "/admin/tarifs");
        }
        tarifsService.definirTarif(moi.getClient_id(), TarifsMapper.toEntity(dto));
        ra.addFlashAttribute("message", "Nouvelle grille tarifaire en vigueur");
        return "redirect:/admin/tarifs";
    }

    // ----------------------------------------------------------------- justificatifs PMR

    @GetMapping("/justificatifs")
    public String justificatifs(Model model) {
        model.addAttribute("justificatifs", placePrioritaireService.getJustificatifsEnAttente());
        return "admin/justificatifs";
    }

    @PostMapping("/justificatifs/{id}/valider")
    public String validerJustificatif(@PathVariable Long id, RedirectAttributes ra) {
        placePrioritaireService.validerJustificatif(id);
        ra.addFlashAttribute("message", "Justificatif validé");
        return "redirect:/admin/justificatifs";
    }

    @PostMapping("/justificatifs/{id}/refuser")
    public String refuserJustificatif(@PathVariable Long id, RedirectAttributes ra) {
        placePrioritaireService.refuserJustificatif(id);
        ra.addFlashAttribute("message", "Justificatif refusé");
        return "redirect:/admin/justificatifs";
    }

    @PostMapping("/places-prioritaires/quota")
    public String quota(RedirectAttributes ra) {
        ra.addFlashAttribute("message", placePrioritaireService.garantirQuotaMinimum() + " place(s) convertie(s) en PRM");
        return "redirect:/admin/places";
    }

    @PostMapping("/places-prioritaires/detection")
    public String detection(RedirectAttributes ra) {
        ra.addFlashAttribute("message", placePrioritaireService.detecterOccupationIllegitime() + " alerte(s) générée(s)");
        return "redirect:/admin/alertes";
    }

    // ----------------------------------------------------------------- paiements en attente

    @GetMapping("/paiements")
    public String paiements(Model model) {
        model.addAttribute("paiements", paiementService.getPaiementsParStatut(StatutPaiement.EN_ATTENTE));
        return "admin/paiements";
    }

    @PostMapping("/paiements/{id}/valider")
    public String validerPaiement(@PathVariable Long id, RedirectAttributes ra) {
        paiementService.validerPaiement(id);
        ra.addFlashAttribute("message", "Paiement validé");
        return "redirect:/admin/paiements";
    }

    @PostMapping("/paiements/{id}/echec")
    public String echecPaiement(@PathVariable Long id, RedirectAttributes ra) {
        paiementService.marquerEchoue(id);
        ra.addFlashAttribute("message", "Paiement marqué comme échoué");
        return "redirect:/admin/paiements";
    }

    @PostMapping("/paiements/{id}/rembourser")
    public String rembourser(@PathVariable Long id, @RequestParam(required = false) BigDecimal montant,
                             RedirectAttributes ra) {
        paiementService.rembourser(id, montant);
        ra.addFlashAttribute("message", "Paiement remboursé");
        return "redirect:/admin/paiements";
    }

    // ----------------------------------------------------------------- alertes

    @GetMapping("/alertes")
    public String alertes(Model model) {
        model.addAttribute("alertes", alerteService.getAlertesAdministrateurs());
        return "admin/alertes";
    }

    @PostMapping("/alertes/{id}/lue")
    public String marquerLue(@PathVariable Long id) {
        alerteService.marquerLue(id);
        return "redirect:/admin/alertes";
    }

    private String invalide(BindingResult result, RedirectAttributes ra, String retour) {
        var erreur = result.getFieldErrors().get(0);
        ra.addFlashAttribute("erreur", "Formulaire invalide : " + erreur.getField() + " — " + erreur.getDefaultMessage());
        return "redirect:" + retour;
    }
}
