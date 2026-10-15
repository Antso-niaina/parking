package com.projet.parking.service;

import com.projet.parking.dto.EntreeSortiesDTO;
import com.projet.parking.dto.PlacesDTO;
import com.projet.parking.entity.Clients;
import com.projet.parking.entity.EntreeSorties;
import com.projet.parking.entity.Facture;
import com.projet.parking.entity.Places;
import com.projet.parking.entity.Reservations;
import com.projet.parking.entity.Vehicules;
import com.projet.parking.enumeration.StatutPaiement;
import com.projet.parking.enumeration.StatutPlace;
import com.projet.parking.enumeration.StatutReservation;
import com.projet.parking.enumeration.StatutSortie;
import com.projet.parking.enumeration.StatutUtilisateur;
import com.projet.parking.enumeration.TypeAcces;
import com.projet.parking.enumeration.TypeAnomalie;
import com.projet.parking.enumeration.TypePlace;
import com.projet.parking.enumeration.TypeVehicule;
import com.projet.parking.exception.OperationNonAutoriseeException;
import com.projet.parking.exception.ResourceNotFoundException;
import com.projet.parking.mapper.EntreeSortiesMapper;
import com.projet.parking.repository.EntreeSortiesRepository;
import com.projet.parking.repository.FactureRepository;
import com.projet.parking.repository.PlacesRepository;
import com.projet.parking.repository.VehiculesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AccesService {

    private static final Duration HORIZON_ACCES_LIBRE = Duration.ofHours(2);
    private static final List<StatutReservation> STATUTS_BLOQUANTS =
            List.of(StatutReservation.EN_ATTENTE, StatutReservation.CONFIRMEE, StatutReservation.EN_COURS);

    private final VehiculesRepository vehiculesRepository;
    private final EntreeSortiesRepository entreeSortiesRepository;
    private final PlacesRepository placesRepository;
    private final FactureRepository factureRepository;
    private final PlacesService placesService;
    private final ReservationService reservationService;
    private final AbonnementService abonnementService;
    private final TarifsService tarifsService;
    private final AnomalieService anomalieService;
    private final PaiementService paiementService;
    private final PlacePrioritaireService placePrioritaireService;

    public AccesService(VehiculesRepository vehiculesRepository,
                        EntreeSortiesRepository entreeSortiesRepository,
                        PlacesRepository placesRepository,
                        FactureRepository factureRepository,
                        PlacesService placesService,
                        ReservationService reservationService,
                        AbonnementService abonnementService,
                        TarifsService tarifsService,
                        AnomalieService anomalieService,
                        PaiementService paiementService,
                        PlacePrioritaireService placePrioritaireService) {
        this.vehiculesRepository = vehiculesRepository;
        this.entreeSortiesRepository = entreeSortiesRepository;
        this.placesRepository = placesRepository;
        this.factureRepository = factureRepository;
        this.placesService = placesService;
        this.reservationService = reservationService;
        this.abonnementService = abonnementService;
        this.tarifsService = tarifsService;
        this.anomalieService = anomalieService;
        this.paiementService = paiementService;
        this.placePrioritaireService = placePrioritaireService;
    }



    @Transactional
    public EntreeSortiesDTO enregistrerEntree(String plaque) {
        return enregistrerEntree(plaque, TypeVehicule.VOITURE);
    }


    @Transactional
    public EntreeSortiesDTO enregistrerEntree(String plaque, TypeVehicule typeVehicule) {
        return enregistrerEntree(plaque, typeVehicule, null);
    }

    @Transactional
    public EntreeSortiesDTO enregistrerEntree(String plaque, TypeVehicule typeVehicule, Long placeId) {
        return enregistrerEntree(plaque, typeVehicule, placeId, false);
    }

    @Transactional
    public EntreeSortiesDTO enregistrerEntree(String plaque, TypeVehicule typeVehicule,
                                              Long placeId, boolean choixPlaceObligatoire) {
        String immat = normaliser(plaque);
        LocalDateTime maintenant = LocalDateTime.now();


        Vehicules vehicule = vehiculesRepository.trouverParImmatriculation(immat)
                .orElseGet(() -> creerOccasionnel(immat, typeVehicule));
        verifierProprietaire(vehicule);


        if (entreeSortiesRepository.existsPresentByImmatriculation(immat)) {
            signalerAnomalie(TypeAnomalie.ENTREE_DEJA_PRESENT, "Entrée alors que le véhicule est déjà présent : " + immat);
            throw new OperationNonAutoriseeException("Le véhicule " + immat + " est déjà à l'intérieur");
        }


        EntreeSorties session = new EntreeSorties();
        Optional<Reservations> reservation = reservationService.verifierReservationValide(immat, maintenant);
        if (reservation.isPresent()) {
            Reservations r = reservation.get();
            Places prevue = r.getPlaces();
            if (placeId != null && !placeId.equals(prevue.getPlace_id())) {
                throw new OperationNonAutoriseeException("Ce véhicule a une réservation sur la place "
                        + prevue.getNumero() + ", qui doit être utilisée à l'entrée");
            }
            Places place = estUtilisable(prevue)
                    ? prevue
                    : reservationService.proposerPlaceEquivalente(r)
                    .orElseThrow(() -> new OperationNonAutoriseeException("Aucune place disponible pour cette réservation"));
            session.setPlaces(prendrePlace(place));
            session.setTypeAcces(TypeAcces.RESERVATION);
            session.setReservation(r);
            r.setStatut(StatutReservation.EN_COURS);
        } else if (abonnementService.estAbonnementValide(immat, maintenant)) {
            Places place;
            if (placeId != null) {
                place = prendrePlaceSelectionnee(placeId, vehicule, true);
            } else if (choixPlaceObligatoire) {
                throw placeManquante();
            } else {
                place = prendrePlace(trouverPlaceAbonne(vehicule, maintenant));
            }
            session.setPlaces(place);
            session.setTypeAcces(TypeAcces.ABONNEMENT);
            session.setPlaceRepli(estPlaceRepli(place, vehicule));
        } else {
            Places place;
            if (placeId != null) {
                place = prendrePlaceSelectionnee(placeId, vehicule, false);
            } else if (choixPlaceObligatoire) {
                throw placeManquante();
            } else {
                place = prendrePlace(trouverPlaceLibre(vehicule, maintenant));
            }
            session.setPlaces(place);
            session.setTypeAcces(TypeAcces.LIBRE);
            session.setPlaceRepli(estPlaceRepli(place, vehicule));
        }


        session.setVehicules(vehicule);
        session.setDateHeureEntree(maintenant);
        session.setStatut(StatutSortie.EN_COURS);
        return EntreeSortiesMapper.toDto(entreeSortiesRepository.save(session));
    }

    public List<PlacesDTO> getPlacesPourEntree() {
        return placesService.listerPlaces();
    }

    private OperationNonAutoriseeException placeManquante() {
        return new OperationNonAutoriseeException("Choisissez une place libre sur le plan avant de valider l'entrée");
    }




    @Transactional
    public EntreeSortiesDTO enregistrerSortie(String plaque) {
        String immat = normaliser(plaque);
        return cloturer(trouverSessionOuverte(immat), immat, false);
    }


    @Transactional
    public EntreeSortiesDTO gererTicketPerdu(String plaque) {
        String immat = normaliser(plaque);
        EntreeSorties session = trouverSessionOuverte(immat);
        signalerAnomalie(TypeAnomalie.TICKET_PERDU, "Ticket perdu pour " + immat + " (session " + session.getEntreeSortie_id() + ")");
        session.setTicketPerdu(true);
        return cloturer(session, immat, true);
    }


    @Transactional(readOnly = true)
    public BigDecimal calculerMontantDu(String plaque, boolean ticketPerdu) {
        String immat = normaliser(plaque);
        EntreeSorties session = trouverSessionOuverte(immat);
        LocalDateTime maintenant = LocalDateTime.now();
        if (abonnementService.estAbonnementValide(immat, maintenant)) {
            return BigDecimal.ZERO;
        }
        session.setDateHeureSortie(maintenant);
        Facture f = ticketPerdu ? tarifsService.calculerForfaitTicketPerdu(session) : tarifsService.calculer(session);
        return f.getMontantTotal().subtract(dejaRegle(session)).max(BigDecimal.ZERO);
    }

    private EntreeSortiesDTO cloturer(EntreeSorties session, String immat, boolean ticketPerdu) {
        LocalDateTime sortie = LocalDateTime.now();


        if (!sortie.isAfter(session.getDateHeureEntree())) {
            signalerAnomalie(TypeAnomalie.HORLOGE_INCOHERENTE,
                    "Sortie (" + sortie + ") <= entrée (" + session.getDateHeureEntree() + ") pour " + immat);
            throw new OperationNonAutoriseeException("Heure de sortie incohérente");
        }
        session.setDateHeureSortie(sortie);


        if (!abonnementService.estAbonnementValide(immat, sortie)) {
            Facture facture = ticketPerdu
                    ? tarifsService.calculerForfaitTicketPerdu(session)
                    : tarifsService.calculer(session);

            BigDecimal recu = dejaRegle(session);
            if (recu.compareTo(facture.getMontantTotal()) < 0) {
                throw new OperationNonAutoriseeException("Paiement insuffisant : reste "
                        + facture.getMontantTotal().subtract(recu) + " à payer");
            }
            facture.setStatutPaiement(StatutPaiement.PAYEE);
            factureRepository.save(facture);
        }


        placesService.changerStatut(session.getPlaces().getPlace_id(), StatutPlace.LIBRE);
        session.setStatut(StatutSortie.TERMINE);
        if (session.getReservation() != null) {
            session.getReservation().setStatut(StatutReservation.TERMINEE);
        }
        return EntreeSortiesMapper.toDto(session);
    }


    private BigDecimal dejaRegle(EntreeSorties session) {
        BigDecimal recu = paiementService.totalValideSession(session.getEntreeSortie_id());
        Reservations r = session.getReservation();
        return (r != null && r.getMontantPaye() != null) ? recu.add(r.getMontantPaye()) : recu;
    }

    private EntreeSorties trouverSessionOuverte(String immat) {
        return entreeSortiesRepository.findOuverteByImmatriculation(immat).orElseThrow(() -> {
            signalerAnomalie(TypeAnomalie.SORTIE_SANS_ENTREE, "Sortie sans entrée : " + immat);
            return new OperationNonAutoriseeException("Aucune session ouverte pour " + immat);
        });
    }



    public void signalerAnomalie(TypeAnomalie type, String details) {
        anomalieService.signaler(type, details);
    }

    @Transactional(readOnly = true)
    public List<EntreeSortiesDTO> getSessionsActives() {
        return entreeSortiesRepository.findSessionsActives(StatutSortie.EN_COURS)
                .stream().map(EntreeSortiesMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<EntreeSortiesDTO> getHistorique(String plaque) {
        return entreeSortiesRepository.findHistorique(normaliser(plaque))
                .stream().map(EntreeSortiesMapper::toDto).toList();
    }



    private Vehicules creerOccasionnel(String immat, TypeVehicule type) {
        Vehicules v = new Vehicules();
        v.setImmatriculation(immat);
        v.setType_vehicule(type != null ? type : TypeVehicule.VOITURE);
        v.setMarque("INCONNUE");
        v.setModel("INCONNU");
        return vehiculesRepository.save(v);
    }

    private void verifierProprietaire(Vehicules v) {
        Clients c = v.getClients();
        if (c == null) return;
        if (c.getStatut() != StatutUtilisateur.ACTIF) {
            throw new OperationNonAutoriseeException("Accès refusé : compte du propriétaire non actif");
        }
        if (factureRepository.existsImpayeByClientId(c.getClient_id(), StatutPaiement.EN_ATTENTE)) {
            throw new OperationNonAutoriseeException("Accès refusé : le propriétaire a des factures impayées");
        }
    }


    private Places trouverPlaceAbonne(Vehicules v, LocalDateTime maintenant) {
        return placesRepository.trouverDisponibles(List.of(TypePlace.ABONNE), StatutPlace.HORS_SERVICE,
                        STATUTS_BLOQUANTS, maintenant, maintenant.plus(HORIZON_ACCES_LIBRE))
                .stream().findFirst()
                .orElseGet(() -> trouverPlaceLibre(v, maintenant));
    }





    private Places trouverPlaceLibre(Vehicules v, LocalDateTime maintenant) {
        LocalDateTime fin = maintenant.plus(HORIZON_ACCES_LIBRE);


        if (v.getType_vehicule() == TypeVehicule.PMR && placePrioritaireService.estEligible(v.getClients(), v)) {
            Optional<Places> prioritaire = placePrioritaireService.attribuerPlacePrioritaire(v, maintenant, fin);
            if (prioritaire.isPresent()) {
                return prioritaire.get();
            }
        }

        Optional<Places> standard = placesRepository.trouverDisponibles(List.of(TypePlace.STANDARD),
                StatutPlace.HORS_SERVICE, STATUTS_BLOQUANTS, maintenant, fin).stream().findFirst();
        if (standard.isPresent()) {
            return standard.get();
        }

        return placePrioritaireService.appliquerRegleRepli(v, maintenant, fin)
                .orElseThrow(() -> new OperationNonAutoriseeException("Parking complet pour ce type de véhicule"));
    }


    private boolean estPlaceRepli(Places place, Vehicules v) {
        return place.getType_place() == TypePlace.PRM && !placePrioritaireService.estEligible(v.getClients(), v);
    }

    private boolean estUtilisable(Places p) {
        return p.getStatut() != StatutPlace.HORS_SERVICE
                && p.getStatut() != StatutPlace.OCCUPEE
                && !entreeSortiesRepository.existsPresentByPlaceId(p.getPlace_id());
    }


    private Places prendrePlace(Places candidate) {
        Places p = placesRepository.findByIdForUpdate(candidate.getPlace_id())
                .orElseThrow(() -> new ResourceNotFoundException("Place", "avec id inexistant : " + candidate.getPlace_id()));
        if (!estUtilisable(p)) {
            throw new OperationNonAutoriseeException("La place " + p.getNumero() + " vient d'être prise, réessayez");
        }
        return placesService.changerStatut(p.getPlace_id(), StatutPlace.OCCUPEE);
    }

    private Places prendrePlaceSelectionnee(Long placeId, Vehicules vehicule, boolean abonnementActif) {
        Places place = placesRepository.findByIdForUpdate(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Place", "avec id inexistant : " + placeId));
        if (place.getStatut() != StatutPlace.LIBRE || !estUtilisable(place)) {
            throw new OperationNonAutoriseeException("La place " + place.getNumero() + " n'est plus libre");
        }
        boolean placeAbonne = abonnementActif && place.getType_place() == TypePlace.ABONNE;
        if (!placeAbonne && !placesService.estCompatible(place, vehicule)) {
            throw new OperationNonAutoriseeException("La place " + place.getNumero()
                    + " n'est pas compatible avec ce véhicule");
        }
        if (place.getType_place() == TypePlace.PRM) {
            placePrioritaireService.verifierEligibilite(vehicule.getClients(), vehicule);
        }
        return placesService.changerStatut(place.getPlace_id(), StatutPlace.OCCUPEE);
    }

    private String normaliser(String plaque) {
        if (plaque == null || plaque.isBlank()) {
            throw new OperationNonAutoriseeException("La plaque est obligatoire");
        }
        String immat = plaque.trim().toUpperCase();
        if (immat.length() > 10) {
            throw new OperationNonAutoriseeException("Plaque invalide (10 caractères maximum)");
        }
        return immat;
    }
}
