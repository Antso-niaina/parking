package com.projet.parking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // active @PreAuthorize sur les contrôleurs REST et sur les pages
public class SecurityConfig {

    // Requis par UtilisateurService (type concret BCryptPasswordEncoder)
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // =====================================================================
    // 1) API REST (/api/**) : HTTP Basic, sans session ni cookie
    // =====================================================================
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")   // cette chaîne ne s'applique qu'à l'API
                .csrf(AbstractHttpConfigurer::disable)   // API sans session ni cookie
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        // public
                        .requestMatchers(HttpMethod.POST, "/api/auth/inscription").permitAll()

                        // borne d'entrée / sortie
                        .requestMatchers("/api/acces/**").hasAnyRole("BORNE", "ADMIN")

                        // paiements : la validation vient de la borne ou d'un admin, le remboursement d'un admin
                        .requestMatchers(HttpMethod.POST, "/api/paiements/*/valider", "/api/paiements/*/echec")
                        .hasAnyRole("BORNE", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/paiements/*/rembourser").hasRole("ADMIN")

                        // administration
                        .requestMatchers(HttpMethod.GET, "/api/tarifs/actuel").authenticated()
                        .requestMatchers("/api/tarifs/**", "/api/stats/**", "/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/alertes/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/justificatifs/*/valider", "/api/justificatifs/*/refuser")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/clients/*/suspendre", "/api/clients/*/reactiver")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/clients/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/places/**", "/api/zones/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/places/**", "/api/zones/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/places/**", "/api/zones/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/places/**", "/api/zones/**").hasRole("ADMIN")

                        // tout le reste : utilisateur connecté (le détail se règle avec @PreAuthorize)
                        .anyRequest().authenticated()
                );
        return http.build();
    }

    // =====================================================================
    // 2) Pages web (tout le reste) : formulaire de connexion, session, CSRF
    // =====================================================================
    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // public
                        .requestMatchers("/login", "/inscription", "/error",
                                "/css/**", "/js/**", "/images/**").permitAll()

                        // par rôle
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/borne/**").hasAnyRole("BORNE", "ADMIN")
                        .requestMatchers("/espace/**").hasAnyRole("STANDAR", "VIP", "ADMIN")

                        // tout le reste : utilisateur connecté
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll());
        return http.build();
    }
}