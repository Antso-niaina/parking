package com.projet.parking.controller;

import com.projet.parking.entity.Clients;
import com.projet.parking.enumeration.ClientsEnum;
import com.projet.parking.exception.OperationNonAutoriseeException;
import com.projet.parking.repository.ClientsRepository;
import com.projet.parking.service.ClientService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminClientsPageController {

    private final ClientsRepository clientsRepository;
    private final ClientService clientService;

    public AdminClientsPageController(ClientsRepository clientsRepository, ClientService clientService) {
        this.clientsRepository = clientsRepository;
        this.clientService = clientService;
    }

    @GetMapping("/admin/clients")
    public String clients(Model model) {
        model.addAttribute("clients", clientsRepository.findAll().stream()
                .filter(client -> client.getType_client() == ClientsEnum.STANDAR
                        || client.getType_client() == ClientsEnum.VIP)
                .toList());
        return "admin/clients";
    }

    @PostMapping("/admin/clients/{id}/suspendre")
    public String suspendre(@PathVariable Long id, @AuthenticationPrincipal Clients moi,
                            RedirectAttributes ra) {
        return appliquer(id, moi, ra, "Client suspendu", () -> clientService.suspendreUtilisateur(id));
    }

    @PostMapping("/admin/clients/{id}/reactiver")
    public String reactiver(@PathVariable Long id, @AuthenticationPrincipal Clients moi,
                            RedirectAttributes ra) {
        return appliquer(id, moi, ra, "Client réactivé", () -> clientService.reactiverUtilisateur(id));
    }

    @PostMapping("/admin/clients/{id}/supprimer")
    public String supprimer(@PathVariable Long id, @AuthenticationPrincipal Clients moi,
                            RedirectAttributes ra) {
        return appliquer(id, moi, ra, "Compte supprimé", () -> clientService.supprimerUtilisateur(id));
    }

    private String appliquer(Long id, Clients moi, RedirectAttributes ra, String succes, Runnable operation) {
        try {
            if (moi != null && moi.getClient_id().equals(id)) {
                throw new OperationNonAutoriseeException("Vous ne pouvez pas modifier votre propre compte ici");
            }
            operation.run();
            ra.addFlashAttribute("message", succes);
        } catch (OperationNonAutoriseeException e) {
            ra.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/clients";
    }
}
