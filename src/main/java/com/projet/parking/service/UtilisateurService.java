package com.projet.parking.service;

import com.projet.parking.entity.Clients;
import com.projet.parking.enumeration.ClientsEnum;
import com.projet.parking.exception.OperationNonAutoriseeException;
import com.projet.parking.repository.ClientsRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
@Service
@AllArgsConstructor
public class UtilisateurService implements UserDetailsService {
    private final ClientsRepository clientsRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    public Clients inscription(Clients clients){
        Optional<Clients> clientsOptional = this.clientsRepository.findByEmail(clients.getEmail());
        if(clientsOptional.isPresent()){
            throw new OperationNonAutoriseeException("Utilisateur deja existant !");
        }
        String passwordCrypte = this.bCryptPasswordEncoder.encode(clients.getMdp());
        clients.setMdp(passwordCrypte);
        clients.setDate_creation(LocalDate.now());
        clients.setType_client(ClientsEnum.STANDAR);
        return clientsRepository.save(clients);
    }
    @Override
    public Clients loadUserByUsername(String clients) throws UsernameNotFoundException{
        return this.clientsRepository.findByEmail(clients).orElseThrow(()-> new UsernameNotFoundException("Utilisateur inexistants"));
    }
}
