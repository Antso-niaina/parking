package com.projet.parking.service;

import com.projet.parking.entity.*;
import com.projet.parking.enumeration.*;
import com.projet.parking.exception.OperationNonAutoriseeException;
import com.projet.parking.exception.ResourceNotFoundException;
import com.projet.parking.repository.AbonnementsRepository;
import com.projet.parking.repository.ClientsRepository;
import com.projet.parking.repository.TarifsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TarifsService {

    private static final LocalTime DEBUT_JOUR = LocalTime.of(6, 0);
    private static final LocalTime FIN_JOUR = LocalTime.of(20, 0);
    private static final BigDecimal CENT = BigDecimal.valueOf(100);
    private static final BigDecimal PAS_ARRONDI = new BigDecimal("0.05");   // ex. 50 pour des Ariary
    private static final LocalDateTime FIN_DES_TEMPS = LocalDateTime.of(9999, 12, 31, 23, 59, 59);
    private static final List<StatutAbonnement> ABONNEMENTS_ELIGIBLES =
            List.of(StatutAbonnement.ACTIF, StatutAbonnement.RENOUVELE);

    private final TarifsRepository tarifsRepository;
    private final AbonnementsRepository abonnementsRepository;
    private final ClientsRepository clientsRepository;

    public TarifsService(TarifsRepository tarifsRepository,
                         AbonnementsRepository abonnementsRepository,
                         ClientsRepository clientsRepository) {
        this.tarifsRepository = tarifsRepository;
        this.abonnementsRepository = abonnementsRepository;
        this.clientsRepository = clientsRepository;
    }

    // ===================================================================== CALCUL

    /** Facture non enregistrée, calculée à partir de session.getDateHeureSortie(). */
    @Transactional(readOnly = true)
    public Facture calculer(EntreeSorties session) {
        LocalDateTime entree = session.getDateHeureEntree();
        LocalDateTime sortie = session.getDateHeureSortie();
        Tarifs tarif = tarifPourSejour(entree, sortie);
        Vehicules vehicule = session.getVehicules();

        BigDecimal base = calculerBase(session, tarif);
        BigDecimal apresRemise = appliquerRemise(base, vehicule.getClients(), vehicule, tarif, sortie);
        BigDecimal remise = base.subtract(apresRemise);

        // pénalité de dépassement si la session vient d'une réservation
        int prevue = 0;
        long depassement = 0;
        BigDecimal penalite = BigDecimal.ZERO;
        Reservations r = session.getReservation();
        if (r != null) {
            prevue = (int) Duration.between(r.getDate_heure_arrive(), r.getDate_heure_depart()).toMinutes();
            depassement = Math.max(0, Duration.between(r.getDate_heure_depart(), sortie).toMinutes());
            penalite = penalite(tarif, TypePenalite.DEPASSEMENT).multiply(BigDecimal.valueOf((depassement + 59) / 60));
        }

        BigDecimal total = arrondir(apresRemise.add(penalite));   // les pénalités ne sont jamais remisées
        return construire(session, tarif, base, remise, penalite, total, prevue, depassement);
    }

    /** Montant seul, utile pour afficher le prix à la borne avant le paiement. */
    @Transactional(readOnly = true)
    public BigDecimal calculerMontant(EntreeSorties session) {
        return calculer(session).getMontantTotal();
    }

    /** Ticket perdu : forfait fixe de la grille, sans remise. */
    @Transactional(readOnly = true)
    public Facture calculerForfaitTicketPerdu(EntreeSorties session) {
        Tarifs tarif = tarifPourSejour(session.getDateHeureEntree(), session.getDateHeureSortie());
        BigDecimal forfait = penalite(tarif, TypePenalite.TICKET_PERDU);
        return construire(session, tarif, forfait, BigDecimal.ZERO, BigDecimal.ZERO, arrondir(forfait), 0, 0);
    }

    /**
     * Durée facturable après franchise, découpée en heures entamées. Chaque heure prend le tarif de la
     * période où elle commence (nuit / week-end), multiplié par les coefficients véhicule et place.
     * Le plafond journalier s'applique par tranche de 24 h.
     */
    private BigDecimal calculerBase(EntreeSorties session, Tarifs tarif) {
        LocalDateTime entree = session.getDateHeureEntree();
        LocalDateTime sortie = session.getDateHeureSortie();

        Duration facturable = appliquerFranchise(Duration.between(entree, sortie), tarif);
        if (facturable.isZero()) {
            return BigDecimal.ZERO;
        }
        long blocs = (facturable.toMinutes() + 59) / 60;
        LocalDateTime debutFacturation = entree.plusMinutes(tarif.getFranchiseMinutes());

        BigDecimal coef = tarif.getCoefVehicule().getOrDefault(session.getVehicules().getType_vehicule(), BigDecimal.ONE)
                .multiply(tarif.getCoefPlace().getOrDefault(session.getPlaces().getType_place(), BigDecimal.ONE));
        BigDecimal tauxHoraire = tarif.getTarifHoraire().multiply(coef);
        BigDecimal plafond = tarif.getForfaitJournalier().multiply(coef);

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal tranche = BigDecimal.ZERO;
        long trancheCourante = 0;
        for (long i = 0; i < blocs; i++) {
            if (i / 24 != trancheCourante) {
                total = total.add(appliquerPlafondJournalier(tranche, plafond));
                tranche = BigDecimal.ZERO;
                trancheCourante = i / 24;
            }
            LocalDateTime debutBloc = debutFacturation.plusHours(i);
            tranche = tranche.add(tauxHoraire.multiply(coefPeriode(tarif, debutBloc)));
        }
        return total.add(appliquerPlafondJournalier(tranche, plafond));
    }

    /** Les premières minutes sont offertes : on les retire de la durée facturable. */
    public Duration appliquerFranchise(Duration duree, Tarifs tarif) {
        return Duration.ofMinutes(Math.max(0, duree.toMinutes() - tarif.getFranchiseMinutes()));
    }

    public BigDecimal appliquerPlafondJournalier(BigDecimal montantJour, BigDecimal plafond) {
        return montantJour.min(plafond);
    }

    /** Une seule remise, la plus avantageuse (abonné, PMR ou VIP) : pas de cumul. */
    public BigDecimal appliquerRemise(BigDecimal montant, Clients client, Vehicules vehicule,
                                      Tarifs tarif, LocalDateTime instant) {
        double pct = 0;
        if (vehicule != null && vehicule.getType_vehicule() == TypeVehicule.PMR) {
            pct = Math.max(pct, tarif.getReductionPmrPourcentage());
        }
        if (client != null) {
            if (client.getType_client() == ClientsEnum.VIP) {
                pct = Math.max(pct, tarif.getReductionVipPourcentage());
            }
            if (abonnementsRepository.existsActiveByClientId(client.getClient_id(), ABONNEMENTS_ELIGIBLES, instant)) {
                pct = Math.max(pct, tarif.getReductionAbonnePourcentage());
            }
        }
        if (pct <= 0) {
            return montant;
        }
        return montant.multiply(BigDecimal.ONE.subtract(BigDecimal.valueOf(pct).divide(CENT))).max(BigDecimal.ZERO);
    }

    /** DEPASSEMENT = par heure entamée ; les autres types sont des montants fixes. */
    @Transactional(readOnly = true)
    public BigDecimal calculerPenalite(TypePenalite type) {
        LocalDateTime maintenant = LocalDateTime.now();
        return arrondir(penalite(tarifPourSejour(maintenant, maintenant), type));
    }

    // ===================================================================== REMBOURSEMENT

    public BigDecimal calculerRemboursement(Reservations reservation) {
        return calculerRemboursement(reservation, LocalDateTime.now());
    }

    /** 100 % si plus de 24 h avant l'arrivée, 50 % si plus de 2 h, sinon rien. */
    public BigDecimal calculerRemboursement(Reservations r, LocalDateTime maintenant) {
        if (r.getMontantPaye() == null || r.getMontantPaye().signum() == 0
                || (r.getStatut() != StatutReservation.EN_ATTENTE && r.getStatut() != StatutReservation.CONFIRMEE)) {
            return BigDecimal.ZERO.setScale(2);
        }
        Duration delai = Duration.between(maintenant, r.getDate_heure_arrive());
        BigDecimal taux;
        if (delai.compareTo(Duration.ofHours(24)) >= 0) taux = BigDecimal.ONE;
        else if (delai.compareTo(Duration.ofHours(2)) >= 0) taux = new BigDecimal("0.50");
        else taux = BigDecimal.ZERO;
        return arrondir(r.getMontantPaye().multiply(taux));
    }

    // ===================================================================== ADMINISTRATION

    /** Nouvelle grille en vigueur immédiatement ; les anciennes sont clôturées, jamais supprimées. */
    @Transactional
    public Tarifs definirTarif(Long adminId, Tarifs grille) {
        Clients admin = clientsRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "avec id inexistant : " + adminId));
        if (admin.getType_client() != ClientsEnum.ADMIN) {
            throw new OperationNonAutoriseeException("Seul un administrateur peut définir une grille tarifaire");
        }
        valider(grille);

        LocalDateTime maintenant = LocalDateTime.now();
        for (Tarifs ancien : tarifsRepository.findNonTermines(maintenant)) {
            ancien.setDateFinValidite(ancien.getDateDebutValidite().isAfter(maintenant)
                    ? ancien.getDateDebutValidite() : maintenant);
            ancien.setActif(false);
        }

        grille.setTarif_id(null);
        grille.setFactures(new ArrayList<>());
        grille.setDateDebutValidite(maintenant);
        grille.setDateFinValidite(FIN_DES_TEMPS);
        grille.setActif(true);
        return tarifsRepository.save(grille);
    }

    @Transactional(readOnly = true)
    public List<Tarifs> getHistoriqueTarifs() {
        return tarifsRepository.findAllByOrderByDateDebutValiditeDesc();
    }

    private void valider(Tarifs g) {
        if (g == null || g.getTarifHoraire() == null || g.getForfaitJournalier() == null
                || g.getTarifHoraire().signum() <= 0) {
            throw new OperationNonAutoriseeException("Tarif horaire et forfait journalier sont obligatoires");
        }
        if (g.getForfaitJournalier().compareTo(g.getTarifHoraire()) < 0) {
            throw new OperationNonAutoriseeException("Le forfait journalier doit être >= au tarif horaire");
        }
        if (g.getFranchiseMinutes() < 0) {
            throw new OperationNonAutoriseeException("La franchise ne peut pas être négative");
        }
        double[] pourcentages = {g.getReductionAbonnePourcentage(), g.getReductionPmrPourcentage(),
                g.getReductionVipPourcentage(), g.getMajorationNuitPourcentage(),
                g.getMajorationWeekendPourcentage()};
        for (double p : pourcentages) {
            if (p < 0 || p > 100) {
                throw new OperationNonAutoriseeException("Les pourcentages doivent être compris entre 0 et 100");
            }
        }
        for (BigDecimal pen : new BigDecimal[]{g.getPenaliteDepassementHoraire(), g.getPenaliteNoShow(),
                g.getPenaliteTicketPerdu(), g.getPenaliteMauvaisePlace()}) {
            if (pen != null && pen.signum() < 0) {
                throw new OperationNonAutoriseeException("Une pénalité ne peut pas être négative");
            }
        }
        if (g.getCoefVehicule().values().stream().anyMatch(c -> c.signum() <= 0)
                || g.getCoefPlace().values().stream().anyMatch(c -> c.signum() <= 0)) {
            throw new OperationNonAutoriseeException("Les coefficients doivent être strictement positifs");
        }
    }

    // ===================================================================== OUTILS

    /** Règle fixe : au pas de PAS_ARRONDI le plus proche, jamais négatif. */
    public BigDecimal arrondir(BigDecimal montant) {
        if (montant == null || montant.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return montant.divide(PAS_ARRONDI, 0, RoundingMode.HALF_UP).multiply(PAS_ARRONDI).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal penalite(Tarifs t, TypePenalite type) {
        return switch (type) {
            case DEPASSEMENT -> nz(t.getPenaliteDepassementHoraire());
            case NO_SHOW -> nz(t.getPenaliteNoShow());
            case MAUVAISE_PLACE -> nz(t.getPenaliteMauvaisePlace());
            case TICKET_PERDU -> t.getPenaliteTicketPerdu() != null && t.getPenaliteTicketPerdu().signum() > 0
                    ? t.getPenaliteTicketPerdu() : t.getForfaitJournalier();
        };
    }

    /** Majoration la plus forte entre nuit et week-end (pas de cumul). */
    private BigDecimal coefPeriode(Tarifs t, LocalDateTime instant) {
        double pct = 0;
        LocalTime h = instant.toLocalTime();
        if (h.isBefore(DEBUT_JOUR) || !h.isBefore(FIN_JOUR)) {
            pct = t.getMajorationNuitPourcentage();
        }
        DayOfWeek jour = instant.getDayOfWeek();
        if (jour == DayOfWeek.SATURDAY || jour == DayOfWeek.SUNDAY) {
            pct = Math.max(pct, t.getMajorationWeekendPourcentage());
        }
        return BigDecimal.ONE.add(BigDecimal.valueOf(pct).divide(CENT));
    }

    /** Grille en vigueur à l'entrée (le prix annoncé à l'arrivée est tenu) ; à défaut, celle de la sortie. */
    private Tarifs tarifPourSejour(LocalDateTime entree, LocalDateTime sortie) {
        return tarifsRepository.findEnVigueur(entree).stream().findFirst()
                .or(() -> tarifsRepository.findEnVigueur(sortie).stream().findFirst())
                .orElseThrow(() -> new ResourceNotFoundException("Tarif", "en vigueur introuvable"));
    }

    private Facture construire(EntreeSorties session, Tarifs tarif, BigDecimal base, BigDecimal remise,
                               BigDecimal penalite, BigDecimal total, int prevue, long depassement) {
        Facture f = new Facture();
        f.setEntreeSorties(session);
        f.setTarifs(tarif);
        f.setDureePrevueMinute(prevue);
        f.setDureeReelleMinute((int) Duration.between(session.getDateHeureEntree(), session.getDateHeureSortie()).toMinutes());
        f.setDureeDepassementMinutes((int) depassement);
        f.setMontantBase(base.setScale(2, RoundingMode.HALF_UP));
        f.setMontantMajoration(penalite.setScale(2, RoundingMode.HALF_UP));
        f.setMontantReduction(remise.setScale(2, RoundingMode.HALF_UP));
        f.setMontantTotal(total);
        f.setStatutPaiement(StatutPaiement.EN_ATTENTE);
        f.setDateFacturation(session.getDateHeureSortie().toLocalDate());
        return f;
    }
    @Transactional(readOnly = true)
    public Tarifs getTarifActuel() {
        LocalDateTime maintenant = LocalDateTime.now();
        return tarifPourSejour(maintenant, maintenant);
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}