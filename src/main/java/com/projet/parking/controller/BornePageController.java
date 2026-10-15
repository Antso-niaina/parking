package com.projet.parking.controller;

import com.projet.parking.dto.*;
import com.projet.parking.enumeration.*;
import com.projet.parking.exception.*;
import com.projet.parking.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/borne")
public class BornePageController {

    @Data public static class EntreeForm {
        @NotBlank private String plaque;
        private TypeVehicule typeVehicule;
        private Long placeId;
        private boolean utiliserReservation;
    }

    @Data public static class SortieForm {
        @NotBlank private String plaque;
        private boolean ticketPerdu;
    }

    @Data public static class AnomalieForm {
        @NotNull private TypeAnomalie type;
        @NotBlank private String details;
    }

    private final AccesService accesService;
    private final PaiementService paiementService;

    public BornePageController(AccesService accesService, PaiementService paiementService) {
        this.accesService = accesService;
        this.paiementService = paiementService;
    }

    @GetMapping
    public String accueil(Model model) {
        model.addAttribute("sessions", accesService.getSessionsActives());
        model.addAttribute("entree", new EntreeForm());
        model.addAttribute("sortie", new SortieForm());
        model.addAttribute("anomalie", new AnomalieForm());
        model.addAttribute("typesVehicule", TypeVehicule.values());
        model.addAttribute("places", accesService.getPlacesPourEntree());
        model.addAttribute("typesAnomalie", TypeAnomalie.values());
        return "borne/accueil";
    }

    @PostMapping("/entree")
    public String entree(@Valid @ModelAttribute("entree") EntreeForm form, BindingResult result,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            return invalide(result, ra);
        }
        EntreeSortiesDTO session = accesService.enregistrerEntree(
                form.getPlaque(), form.getTypeVehicule(), form.getPlaceId(), true);
        ra.addFlashAttribute("message", "Entrée enregistrée pour " + session.getImmatriculation()
                + " (session n° " + session.getEntreeSortie_id() + ")");
        return "redirect:/borne";
    }


    @PostMapping("/sortie/montant")
    public String montantSortie(@Valid @ModelAttribute("sortie") SortieForm form, BindingResult result,
                                Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return invalide(result, ra);
        }
        model.addAttribute("montantDu", accesService.calculerMontantDu(form.getPlaque(), form.isTicketPerdu()));
        return "borne/sortie";
    }

    @GetMapping({"/sortie/montant", "/sortie"})
    public String accesDirectSortie() {
        return "redirect:/borne";
    }


    @PostMapping("/sortie")
    public String sortie(@Valid @ModelAttribute("sortie") SortieForm form, BindingResult result,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            return invalide(result, ra);
        }
        BigDecimal du = accesService.calculerMontantDu(form.getPlaque(), form.isTicketPerdu());
        if (du.signum() > 0) {
            PaiementDTO paiement = paiementService.creerPaiement(du, TypePaiement.SESSION, sessionActiveId(form.getPlaque()));
            paiementService.validerPaiement(paiement.getPaiement_id());
        }
        EntreeSortiesDTO session = form.isTicketPerdu()
                ? accesService.gererTicketPerdu(form.getPlaque())
                : accesService.enregistrerSortie(form.getPlaque());
        ra.addFlashAttribute("message", "Sortie enregistrée pour " + session.getImmatriculation()
                + (du.signum() > 0 ? " — encaissé : " + du : " — aucun paiement (abonné à jour)"));
        return "redirect:/borne";
    }

    @PostMapping("/anomalies")
    public String anomalie(@Valid @ModelAttribute("anomalie") AnomalieForm form, BindingResult result,
                           RedirectAttributes ra) {
        if (result.hasErrors()) {
            return invalide(result, ra);
        }
        accesService.signalerAnomalie(form.getType(), form.getDetails());
        ra.addFlashAttribute("message", "Anomalie signalée");
        return "redirect:/borne";
    }

    @GetMapping("/historique")
    public String historique(@RequestParam String plaque, Model model) {
        model.addAttribute("plaque", plaque);
        model.addAttribute("sessions", accesService.getHistorique(plaque));
        return "borne/historique";
    }

    private Long sessionActiveId(String plaque) {
        String immat = plaque.trim().toUpperCase();
        return accesService.getSessionsActives().stream()
                .filter(s -> s.getImmatriculation().equalsIgnoreCase(immat))
                .map(EntreeSortiesDTO::getEntreeSortie_id)
                .findFirst()
                .orElseThrow(() -> new OperationNonAutoriseeException("Aucune session ouverte pour " + immat));
    }

    private String invalide(BindingResult result, RedirectAttributes ra) {
        var erreur = result.getFieldErrors().get(0);
        ra.addFlashAttribute("erreur", "Formulaire invalide : " + erreur.getField() + " — " + erreur.getDefaultMessage());
        return "redirect:/borne";
    }
}
