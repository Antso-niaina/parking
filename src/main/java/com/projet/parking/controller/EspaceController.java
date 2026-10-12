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
@RequestMapping("/espace")
public class EspaceController {

    @Data public static class VehiculeForm {
        @NotBlank @Size(max = 10) private String immatriculation;
        @NotNull private TypeVehicule type;
        @NotBlank private String marque;
        @NotBlank private String model;
    }

    @Data public static class ReservationForm {
        @NotNull private Long vehiculeId;
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) private LocalDateTime debut;
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) private LocalDateTime fin;
    }

    @Data public static class AbonnementForm {
        @NotEmpty private List<Long> vehiculeIds;
        @NotNull private FormuleAbonnement formule;
        @NotNull private TypeAbonnements duree;
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) private LocalDateTime dateDebut;
        private boolean renouvellementAuto;
    }

    @Data public static class PaiementForm {
        @NotNull @Positive private BigDecimal montant;
        @NotNull private TypePaiement typePaiement;
        @NotNull private Long cibleId;
    }

    @Data public static class JustificatifForm {
        @NotBlank private String numeroCarte;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate dateExpiration;
    }

    private final VehiculesService vehiculesService;
    private final ReservationService reservationService;
    private final AbonnementService abonnementService;
    private final PaiementService paiementService;
    private final FactureService factureService;
    private final FactureService factures;
    private final AlerteService alerteService;
    private final PlacesService placesService;
    private final PlacePrioritaireService placePrioritaireService;
    private final SecuriteService securite;

    public EspaceController(VehiculesService vehiculesService, ReservationService reservationService,
                            AbonnementService abonnementService, PaiementService paiementService,
                            FactureService factureService, AlerteService alerteService,
                            PlacesService placesService, PlacePrioritaireService placePrioritaireService,
                            SecuriteService securite) {
        this.vehiculesService = vehiculesService;
        this.reservationService = reservationService;
        this.abonnementService = abonnementService;
        this.paiementService = paiementService;
        this.factureService = factureService;
        this.factures = factureService;
        this.alerteService = alerteService;
        this.placesService = placesService;
        this.placePrioritaireService = placePrioritaireService;
        this.securite = securite;
    }

    // ----------------------------------------------------------------- accueil

    @GetMapping
    public String accueil(@AuthenticationPrincipal Clients moi, Model model) {
        Long id = moi.getClient_id();
        model.addAttribute("vehicules", vehiculesService.getVehiculesParUtilisateur(id));
        model.addAttribute("reservations", reservationService.getReservationsParUtilisateur(id));
        model.addAttribute("abonnements", abonnementService.getAbonnementsParUtilisateur(id));
        model.addAttribute("alertes", alerteService.getAlertesUtilisateur(id));
        model.addAttribute("impayes", factureService.getImpayes(id));
        return "espace/accueil";
    }

    // ----------------------------------------------------------------- véhicules

    @GetMapping("/vehicules")
    public String vehicules(@AuthenticationPrincipal Clients moi, Model model) {
        model.addAttribute("vehicules", vehiculesService.getVehiculesParUtilisateur(moi.getClient_id()));
        model.addAttribute("vehicule", new VehiculeForm());
        model.addAttribute("types", TypeVehicule.values());
        return "espace/vehicules";
    }

    @PostMapping("/vehicules")
    public String ajouterVehicule(@AuthenticationPrincipal Clients moi,
                                  @Valid @ModelAttribute("vehicule") VehiculeForm form,
                                  BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return invalide(result, ra, "/espace/vehicules");
        }
        VehiculesDTO dto = new VehiculesDTO();
        dto.setImmatriculation(form.getImmatriculation());
        dto.setType_vehicule(form.getType());
        dto.setMarque(form.getMarque());
        dto.setModel(form.getModel());
        dto.setClient_id(moi.getClient_id());   // toujours le compte connecté
        vehiculesService.enregistrerVehicule(dto);
        ra.addFlashAttribute("message", "Véhicule enregistré");
        return "redirect:/espace/vehicules";
    }

    @PostMapping("/vehicules/{id}/supprimer")
    @PreAuthorize("@securite.proprietaireVehicule(#id, authentication)")
    public String supprimerVehicule(@PathVariable Long id, RedirectAttributes ra) {
        vehiculesService.supprimerVehicule(id);
        ra.addFlashAttribute("message", "Véhicule supprimé");
        return "redirect:/espace/vehicules";
    }

    // ----------------------------------------------------------------- réservations

    @GetMapping("/reservations")
    public String reservations(@AuthenticationPrincipal Clients moi, Model model) {
        model.addAttribute("reservations", reservationService.getReservationsParUtilisateur(moi.getClient_id()));
        model.addAttribute("vehicules", vehiculesService.getVehiculesParUtilisateur(moi.getClient_id()));
        model.addAttribute("reservation", new ReservationForm());
        return "espace/reservations";
    }

    /** La place est choisie automatiquement : première place compatible libre sur le créneau. */
    @PostMapping("/reservations")
    public String reserver(@AuthenticationPrincipal Clients moi,
                           @Valid @ModelAttribute("reservation") ReservationForm form,
                           BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return invalide(result, ra, "/espace/reservations");
        }
        VehiculesDTO vehicule = vehiculesService.getVehiculesParUtilisateur(moi.getClient_id()).stream()
                .filter(v -> v.getVehicule_id().equals(form.getVehiculeId()))
                .findFirst()
                .orElseThrow(() -> new OperationNonAutoriseeException("Véhicule introuvable"));
        Places place = placesService.trouverPlaceDisponible(vehicule.getType_vehicule(), form.getDebut(), form.getFin());
        reservationService.creerReservation(moi.getClient_id(), vehicule.getVehicule_id(),
                place.getPlace_id(), form.getDebut(), form.getFin());
        ra.addFlashAttribute("message", "Réservation créée (place " + place.getNumero() + "). Elle sera confirmée après paiement.");
        return "redirect:/espace/reservations";
    }

    @PostMapping("/reservations/{id}/annuler")
    @PreAuthorize("@securite.proprietaireReservation(#id, authentication)")
    public String annulerReservation(@PathVariable Long id, RedirectAttributes ra) {
        RemboursementDTO r = paiementService.annulerReservationEtRembourser(id);
        ra.addFlashAttribute("message", "Réservation annulée. Montant remboursé : " + r.montantRembourse());
        return "redirect:/espace/reservations";
    }

    // ----------------------------------------------------------------- abonnements

    @GetMapping("/abonnements")
    public String abonnements(@AuthenticationPrincipal Clients moi, Model model) {
        model.addAttribute("abonnements", abonnementService.getAbonnementsParUtilisateur(moi.getClient_id()));
        model.addAttribute("vehicules", vehiculesService.getVehiculesParUtilisateur(moi.getClient_id()));
        model.addAttribute("abonnement", new AbonnementForm());
        model.addAttribute("formules", FormuleAbonnement.values());
        model.addAttribute("durees", TypeAbonnements.values());
        return "espace/abonnements";
    }

    @PostMapping("/abonnements")
    public String souscrire(@AuthenticationPrincipal Clients moi,
                            @Valid @ModelAttribute("abonnement") AbonnementForm form,
                            BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return invalide(result, ra, "/espace/abonnements");
        }
        abonnementService.souscrire(moi.getClient_id(), form.getVehiculeIds(), form.getFormule(),
                form.getDateDebut(), form.getDuree(), form.isRenouvellementAuto());
        ra.addFlashAttribute("message", "Abonnement créé. Il sera actif après paiement.");
        return "redirect:/espace/abonnements";
    }

    @PostMapping("/abonnements/{id}/renouveler")
    @PreAuthorize("@securite.proprietaireAbonnement(#id, authentication)")
    public String renouveler(@PathVariable Long id, RedirectAttributes ra) {
        abonnementService.renouveler(id);
        ra.addFlashAttribute("message", "Abonnement renouvelé. La nouvelle période attend son paiement.");
        return "redirect:/espace/abonnements";
    }

    @PostMapping("/abonnements/{id}/resilier")
    @PreAuthorize("@securite.proprietaireAbonnement(#id, authentication)")
    public String resilier(@PathVariable Long id, RedirectAttributes ra) {
        RemboursementDTO r = paiementService.resilierAbonnementEtRembourser(id);
        ra.addFlashAttribute("message", "Abonnement résilié. Montant remboursé : " + r.montantRembourse());
        return "redirect:/espace/abonnements";
    }

    // ----------------------------------------------------------------- paiements

    @GetMapping("/paiements")
    public String paiements(@AuthenticationPrincipal Clients moi, Model model) {
        model.addAttribute("paiements", paiementService.getPaiementsParUtilisateur(moi.getClient_id()));
        model.addAttribute("paiement", new PaiementForm());
        model.addAttribute("typesPaiement", List.of(TypePaiement.RESERVATION, TypePaiement.ABONNEMENT));
        return "espace/paiements";
    }

    @PostMapping("/paiements")
    public String payer(@Valid @ModelAttribute("paiement") PaiementForm form, BindingResult result,
                        Authentication auth, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return invalide(result, ra, "/espace/paiements");
        }
        if (!securite.peutCreerPaiement(form.getTypePaiement(), form.getCibleId(), auth)) {
            throw new AccessDeniedException("Paiement non autorisé");
        }
        paiementService.creerPaiement(form.getMontant(), form.getTypePaiement(), form.getCibleId());
        ra.addFlashAttribute("message", "Paiement enregistré, en attente de validation");
        return "redirect:/espace/paiements";
    }

    @GetMapping("/paiements/{id}/recu")
    @PreAuthorize("@securite.proprietairePaiement(#id, authentication)")
    public String recu(@PathVariable Long id, Model model) {
        model.addAttribute("recu", factures.genererRecu(id));
        return "espace/recu";
    }

    // ----------------------------------------------------------------- alertes

    @GetMapping("/alertes")
    public String alertes(@AuthenticationPrincipal Clients moi, Model model) {
        model.addAttribute("alertes", alerteService.getAlertesUtilisateur(moi.getClient_id()));
        return "espace/alertes";
    }

    @PostMapping("/alertes/{id}/lue")
    @PreAuthorize("@securite.proprietaireAlerte(#id, authentication)")
    public String marquerLue(@PathVariable Long id) {
        alerteService.marquerLue(id);
        return "redirect:/espace/alertes";
    }

    // ----------------------------------------------------------------- justificatif PMR

    @GetMapping("/justificatif")
    public String justificatif(@AuthenticationPrincipal Clients moi, Model model) {
        model.addAttribute("justificatifs", placePrioritaireService.getJustificatifsParUtilisateur(moi.getClient_id()));
        model.addAttribute("justificatif", new JustificatifForm());
        return "espace/justificatif";
    }

    @PostMapping("/justificatif")
    public String deposer(@AuthenticationPrincipal Clients moi,
                          @Valid @ModelAttribute("justificatif") JustificatifForm form,
                          BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return invalide(result, ra, "/espace/justificatif");
        }
        placePrioritaireService.enregistrerJustificatif(moi.getClient_id(), form.getNumeroCarte(), form.getDateExpiration());
        ra.addFlashAttribute("message", "Justificatif déposé, en attente de vérification");
        return "redirect:/espace/justificatif";
    }

    // ----------------------------------------------------------------- outil

    private String invalide(BindingResult result, RedirectAttributes ra, String retour) {
        var erreur = result.getFieldErrors().get(0);
        ra.addFlashAttribute("erreur", "Formulaire invalide : " + erreur.getField() + " — " + erreur.getDefaultMessage());
        return "redirect:" + retour;
    }
}
