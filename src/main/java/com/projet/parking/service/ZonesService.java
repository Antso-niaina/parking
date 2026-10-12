package com.projet.parking.service;

import com.projet.parking.dto.PlacesDTO;
import com.projet.parking.dto.ZonesDTO;
import com.projet.parking.entity.Zones;
import com.projet.parking.exception.OperationNonAutoriseeException;
import com.projet.parking.exception.ResourceNotFoundException;
import com.projet.parking.mapper.PlacesMapper;
import com.projet.parking.mapper.ZonesMapper;
import com.projet.parking.repository.PlacesRepository;
import com.projet.parking.repository.ZonesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ZonesService {

    private final ZonesRepository zonesRepository;
    private final PlacesRepository placesRepository;

    public ZonesService(ZonesRepository zonesRepository, PlacesRepository placesRepository) {
        this.zonesRepository = zonesRepository;
        this.placesRepository = placesRepository;
    }

    @Transactional
    public ZonesDTO creerZone(ZonesDTO dto) {
        valider(dto);
        String nom = dto.getNom().trim();
        if (zonesRepository.existsByNomIgnoreCase(nom)) {
            throw new OperationNonAutoriseeException("Une zone portant ce nom existe déjà");
        }
        Zones zone = ZonesMapper.toEntity(dto);
        zone.setNom(nom);
        return ZonesMapper.toDto(zonesRepository.save(zone));
    }

    @Transactional
    public ZonesDTO modifierZone(Long id, ZonesDTO dto) {
        valider(dto);
        Zones zone = zonesRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone", "avec id inexistant : " + id));
        String nom = dto.getNom().trim();
        if (zonesRepository.existsNomPourAutre(nom, id)) {
            throw new OperationNonAutoriseeException("Une zone portant ce nom existe déjà");
        }
        long nbPlaces = placesRepository.compterParZone(id);
        if (dto.getCapaciteTotal() < nbPlaces) {
            throw new OperationNonAutoriseeException("Capacité insuffisante : la zone contient déjà "
                    + nbPlaces + " place(s)");
        }
        zone.setNom(nom);
        zone.setDescription(dto.getDescription());
        zone.setCapaciteTotal(dto.getCapaciteTotal());
        return ZonesMapper.toDto(zone);
    }

    @Transactional(readOnly = true)
    public ZonesDTO getZone(Long id) {
        return ZonesMapper.toDto(trouverEntite(id));
    }

    @Transactional(readOnly = true)
    public List<ZonesDTO> listerZones() {
        return zonesRepository.findAllByOrderByNomAsc().stream().map(ZonesMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<PlacesDTO> getPlacesDeLaZone(Long id) {
        trouverEntite(id);
        return placesRepository.findByZoneId(id).stream().map(PlacesMapper::toDto).toList();
    }

    /** Places à rattacher à une zone (créées avant la gestion des zones). */
    @Transactional(readOnly = true)
    public List<PlacesDTO> getPlacesSansZone() {
        return placesRepository.findByZonesIsNull().stream().map(PlacesMapper::toDto).toList();
    }

    @Transactional
    public void supprimerZone(Long id) {
        Zones zone = zonesRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone", "avec id inexistant : " + id));
        long nbPlaces = placesRepository.compterParZone(id);
        if (nbPlaces > 0) {
            throw new OperationNonAutoriseeException("Suppression refusée : la zone contient " + nbPlaces
                    + " place(s), déplacez-les d'abord");
        }
        zonesRepository.delete(zone);
    }

    private void valider(ZonesDTO dto) {
        if (dto == null || dto.getNom() == null || dto.getNom().isBlank()) {
            throw new OperationNonAutoriseeException("Le nom de la zone est obligatoire");
        }
        if (dto.getCapaciteTotal() < 1) {
            throw new OperationNonAutoriseeException("La capacité totale doit être d'au moins 1");
        }
    }

    private Zones trouverEntite(Long id) {
        return zonesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone", "avec id inexistant : " + id));
    }
}