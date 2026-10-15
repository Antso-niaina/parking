package com.projet.parking.service;

import com.projet.parking.dto.AlerteDTO;
import com.projet.parking.entity.Alerte;
import com.projet.parking.entity.Clients;
import com.projet.parking.entity.Facture;
import com.projet.parking.entity.Reservations;
import com.projet.parking.enumeration.StatutPaiement;
import com.projet.parking.enumeration.StatutReservation;
import com.projet.parking.enumeration.TypeAlerte;
import com.projet.parking.exception.ResourceNotFoundException;
import com.projet.parking.mapper.AlerteMapper;
import com.projet.parking.repository.AlerteRepository;
import com.projet.parking.repository.FactureRepository;
import com.projet.parking.repository.ReservationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class AlerteService {

    private static final Duration RAPPEL_AVANT = Duration.ofHours(1);
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    private final AlerteRepository alerteRepository;
    private final NotificationService notificationService;
    private final ReservationRepository reservationRepository;
    private final FactureRepository factureRepository;

    public AlerteService(AlerteRepository alerteRepository,
                         NotificationService notificationService,
                         ReservationRepository reservationRepository,
                         FactureRepository factureRepository) {
        this.alerteRepository = alerteRepository;
        this.notificationService = notificationService;
        this.reservationRepository = reservationRepository;
        this.factureRepository = factureRepository;
    }




    @Transactional
    public Optional<Alerte> alerterUtilisateur(Clients client, TypeAlerte type, String message, String cle) {
        if (client == null) {
            return Optional.empty();
        }
        Optional<Alerte> alerte = creer(client, type, message, cle);
        alerte.ifPresent(a -> notificationService.notifierUtilisateur(client, titre(type), message));
        return alerte;
    }


    @Transactional
    public Optional<Alerte> alerterAdministrateurs(TypeAlerte type, String message, String cle) {
        Optional<Alerte> alerte = creer(null, type, message, cle);
        alerte.ifPresent(a -> notificationService.notifierAdministrateurs(titre(type), message));
        return alerte;
    }

    private Optional<Alerte> creer(Clients destinataire, TypeAlerte type, String message, String cle) {
        if (cle != null && alerteRepository.existsByCle(cle)) {
            return Optional.empty();
        }
        Alerte a = new Alerte();
        a.setTypeAlerte(type);
        a.setMessage(message.length() > 500 ? message.substring(0, 500) : message);
        a.setDateCreation(LocalDateTime.now());
        a.setCle(cle);
        a.setClients(destinataire);
        return Optional.of(alerteRepository.save(a));
    }




    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public int envoyerRappelsReservation() {
        LocalDateTime maintenant = LocalDateTime.now();
        int envoyes = 0;
        for (Reservations r : reservationRepository.findARappeler(
                StatutReservation.CONFIRMEE, maintenant, maintenant.plus(RAPPEL_AVANT))) {
            String message = "Rappel : votre réservation de la place " + r.getPlaces().getNumero()
                    + " commence le " + r.getDate_heure_arrive().format(FORMAT) + ".";
            if (alerterUtilisateur(r.getClients(), TypeAlerte.RAPPEL_RESERVATION, message,
                    "RAPPEL_RESERVATION:" + r.getReservation_id()).isPresent()) {
                envoyes++;
            }
        }
        return envoyes;
    }


    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public int alerterImpayes() {
        int envoyes = 0;
        for (Facture f : factureRepository.findImpayeesAvecClient(StatutPaiement.EN_ATTENTE)) {
            Clients client = f.getEntreeSorties().getVehicules().getClients();
            String message = "Facture n° " + f.getFacture_id() + " impayée : " + f.getMontantTotal()
                    + " (émise le " + f.getDateFacturation() + ").";
            boolean nouvelle = alerterUtilisateur(client, TypeAlerte.IMPAYE, message,
                    "IMPAYE:" + f.getFacture_id()).isPresent();
            alerterAdministrateurs(TypeAlerte.IMPAYE, client.getNom() + " " + client.getPrenom() + " : " + message,
                    "IMPAYE_ADMIN:" + f.getFacture_id());
            if (nouvelle) {
                envoyes++;
            }
        }
        return envoyes;
    }



    @Transactional(readOnly = true)
    public List<AlerteDTO> getAlertesUtilisateur(Long userId) {
        return alerteRepository.findByDestinataire(userId).stream().map(AlerteMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AlerteDTO> getAlertesAdministrateurs() {
        return alerteRepository.findAdministrateursNonLues().stream().map(AlerteMapper::toDto).toList();
    }

    @Transactional
    public AlerteDTO marquerLue(Long alerteId) {
        Alerte a = alerteRepository.findById(alerteId)
                .orElseThrow(() -> new ResourceNotFoundException("Alerte", "avec id inexistant : " + alerteId));
        a.setLue(true);
        return AlerteMapper.toDto(a);
    }

    private String titre(TypeAlerte type) {
        return switch (type) {
            case ANOMALIE -> "Anomalie détectée";
            case OCCUPATION_ILLEGITIME -> "Occupation illégitime d'une place prioritaire";
            case EXPIRATION_ABONNEMENT -> "Votre abonnement arrive à expiration";
            case RAPPEL_RESERVATION -> "Rappel de réservation";
            case IMPAYE -> "Facture impayée";
        };
    }
}