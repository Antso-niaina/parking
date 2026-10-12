package com.projet.parking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InscriptionDTO {
    @NotBlank(message = "le nom est obligatoire")
    private String nom;
    @NotBlank(message = "le prenom est obligatoire")
    private String prenom;
    @NotBlank(message = "l'email est obligatoire")
    @Email(message = "Le format de l'email doit etre valide")
    private String email;
    @NotBlank(message = "le telephone est obligatoire")
    private String telephone;
    @NotBlank(message = "le mdp est obligatoire")
    @Size(min = 8)
    private String mdp;
}
