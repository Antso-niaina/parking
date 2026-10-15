package com.projet.parking.controller;

import com.projet.parking.service.UtilisateurService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.when;

import com.projet.parking.entity.Clients;
import com.projet.parking.enumeration.ClientsEnum;
import com.projet.parking.enumeration.StatutUtilisateur;
import com.projet.parking.repository.ClientsRepository;
import com.projet.parking.service.ClientService;
import org.springframework.security.test.context.support.WithMockUser;

@WebMvcTest(controllers = {AuthPageController.class, AdminClientsPageController.class})
@AutoConfigureMockMvc
@Import(com.projet.parking.security.SecurityConfig.class)
class AuthPageViewTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UtilisateurService utilisateurService;

    @MockitoBean
    private ClientsRepository clientsRepository;

    @MockitoBean
    private ClientService clientService;

    @Test
    @WithAnonymousUser
    void loginPageRendersToerako() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bienvenue sur Toerako")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"username\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("parking-login.jpg")));
    }

    @Test
    @WithAnonymousUser
    void inscriptionPageExposesForm() throws Exception {
        mockMvc.perform(get("/inscription"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("inscription"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Créer un compte")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminClientsPageRendersClientAccounts() throws Exception {
        Clients client = new Clients();
        client.setClient_id(7L);
        client.setNom("Rabe");
        client.setPrenom("Mina");
        client.setEmail("mina@example.test");
        client.setTelephone("0341234567");
        client.setType_client(ClientsEnum.STANDAR);
        client.setStatut(StatutUtilisateur.ACTIF);
        when(clientsRepository.findAll()).thenReturn(java.util.List.of(client));

        mockMvc.perform(get("/admin/clients"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Gestion des clients")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("mina@example.test")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Suspendre")));
    }
}
