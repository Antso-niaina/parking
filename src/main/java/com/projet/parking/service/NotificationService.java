package com.projet.parking.service;


import com.projet.parking.entity.Clients;
import com.projet.parking.enumeration.ClientsEnum;
import com.projet.parking.enumeration.StatutUtilisateur;
import com.projet.parking.repository.ClientsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class NotificationService {

    private final ClientsRepository clientsRepository;

    public NotificationService(ClientsRepository clientsRepository) {
        this.clientsRepository = clientsRepository;
    }

    public void notifierUtilisateur(Clients client, String sujet, String message) {
        if (client != null) {
            envoyer(client.getEmail(), sujet, message);
        }
    }

    @Transactional(readOnly = true)
    public void notifierAdministrateurs(String sujet, String message) {
        for (Clients admin : clientsRepository.findByTypeAndStatut(ClientsEnum.ADMIN, StatutUtilisateur.ACTIF)) {
            envoyer(admin.getEmail(), sujet, message);
        }
    }

    // Un échec d'envoi ne doit jamais annuler l'opération métier
    private void envoyer(String destinataire, String sujet, String message) {
        try {
            // Pour l'instant : écriture dans les logs. C'est ici qu'on branchera l'e-mail ou le SMS.
            log.info("[NOTIFICATION] à {} | {} | {}", destinataire, sujet, message);
        } catch (RuntimeException e) {
            log.error("Échec d'envoi de notification à {} : {}", destinataire, e.getMessage());
        }
    }
}