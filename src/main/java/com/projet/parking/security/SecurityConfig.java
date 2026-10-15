package com.projet.parking.security;

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
@EnableMethodSecurity
public class SecurityConfig {


    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(HttpMethod.POST, "/api/auth/inscription").permitAll()


                        .requestMatchers("/api/acces/**").hasAnyRole("BORNE", "ADMIN")


                        .requestMatchers(HttpMethod.POST, "/api/paiements/*/valider", "/api/paiements/*/echec")
                        .hasAnyRole("BORNE", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/paiements/*/rembourser").hasRole("ADMIN")


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


                        .anyRequest().authenticated()
                );
        return http.build();
    }


    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/login", "/inscription", "/error",
                                "/css/**", "/js/**", "/images/**").permitAll()


                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/borne/**").hasAnyRole("BORNE", "ADMIN")
                        .requestMatchers("/espace/**").hasAnyRole("STANDAR", "VIP", "ADMIN")


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