package com.projet.parking.restController;

import com.projet.parking.dto.ClientsDTO;
import com.projet.parking.dto.InscriptionDTO;
import com.projet.parking.entity.Clients;
import com.projet.parking.mapper.ClientsMapper;
import com.projet.parking.mapper.InscriptionMapper;
import com.projet.parking.service.UtilisateurService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UtilisateurService utilisateurService;

    public AuthController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @PostMapping("/inscription")
    @ResponseStatus(HttpStatus.CREATED)
    public ClientsDTO inscription(@Valid @RequestBody InscriptionDTO dto) {

        Clients client = utilisateurService.inscription(InscriptionMapper.toEntity(dto, dto.getMdp()));
        return ClientsMapper.toDto(client);
    }

    @GetMapping("/moi")
    public ClientsDTO moi(@AuthenticationPrincipal Clients moi) {
        return ClientsMapper.toDto(moi);
    }
}