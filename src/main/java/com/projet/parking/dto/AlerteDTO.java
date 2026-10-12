package com.projet.parking.dto;

import com.projet.parking.entity.Alerte;
import com.projet.parking.enumeration.TypeAlerte;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlerteDTO {
    private Long alerte_id;
    private TypeAlerte typeAlerte;
    private String message;
    private LocalDateTime dateCreation;
    private boolean lue;
    private Long clientId;
}
