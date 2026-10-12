package com.projet.parking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientsDTO {
    private Long client_id;
    @NotBlank(message = "Le nom ne peut pas etre vide")
    @Size(min = 2,max = 25,message = "Le nom doit contenir entre 2 a 25 caracteres")
    private String nom;
    @NotBlank(message="Le prénom ne peut pas être vide")
    @Size(min=2, max=25, message="Le prénom doit contenir entre 2 à 25 caractères")
    private String prenom;
    @NotBlank(message="L'email ne peut pas être vide")
    @Email(message="Format de l'email invalide")
    private String email;
    @Pattern(regexp="^(0|\\+261)(32|33|34|37|38)\\d{7}$",
            message="Numéro de téléphone malgache invalide")
    private String telephone;
}
