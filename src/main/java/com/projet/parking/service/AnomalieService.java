package com.projet.parking.service;

import com.projet.parking.entity.Anomalie;
import com.projet.parking.enumeration.TypeAlerte;
import com.projet.parking.enumeration.TypeAnomalie;
import com.projet.parking.repository.AnomalieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AnomalieService {
    private final AnomalieRepository anomalieRepository;
    private final AlerteService alerteService;

    public AnomalieService(AnomalieRepository anomalieRepository, AlerteService alerteService) {
        this.anomalieRepository = anomalieRepository;
        this.alerteService = alerteService;
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Anomalie signaler(TypeAnomalie type, String details) {
        Anomalie a = new Anomalie();
        a.setTypeAnomalie(type);
        a.setDetails(details);
        a.setDateHeure(LocalDateTime.now());
        Anomalie saved = anomalieRepository.save(a);

        TypeAlerte typeAlerte = type == TypeAnomalie.OCCUPATION_ILLEGITIME
                ? TypeAlerte.OCCUPATION_ILLEGITIME : TypeAlerte.ANOMALIE;
        alerteService.alerterAdministrateurs(typeAlerte, type + " : " + details, null);
        return saved;
    }
}