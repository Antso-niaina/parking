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
public class AuthPageController {

    private final UtilisateurService utilisateurService;

    public AuthPageController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    /** Envoie chacun vers son espace. */
    @GetMapping("/")
    public String accueil(@AuthenticationPrincipal Clients moi) {
        if (moi == null) {
            return "redirect:/login";
        }
        return switch (moi.getType_client()) {
            case ADMIN -> "redirect:/admin";
            case BORNE -> "redirect:/borne";
            default -> "redirect:/espace";
        };
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/inscription")
    public String formulaire(Model model) {
        model.addAttribute("inscription", new InscriptionDTO());
        return "inscription";
    }

    @PostMapping("/inscription")
    public String inscrire(@Valid @ModelAttribute("inscription") InscriptionDTO dto,
                           BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "inscription";
        }
        try {
            utilisateurService.inscription(InscriptionMapper.toEntity(dto, dto.getMdp()));
        } catch (OperationNonAutoriseeException e) {
            model.addAttribute("erreur", e.getMessage());
            return "inscription";
        }
        return "redirect:/login?inscrit";
    }
}