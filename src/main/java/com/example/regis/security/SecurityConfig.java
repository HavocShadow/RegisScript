package com.example.regis.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

            // =========================
            // CORS
            // =========================
            .cors(cors -> {})

            // =========================
            // CSRF
            // =========================
            .csrf(csrf -> csrf.disable())

            // =========================
            // STATELESS JWT
            // =========================
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            // =========================
            // AUTHORIZATION
            // =========================
            .authorizeHttpRequests(auth -> auth

                // Public endpoints
                .requestMatchers(
                    "/api/v1/auth/login",
                    "/api/v1/auth/register"
                ).permitAll()

                // Admin endpoints
                .requestMatchers(
                    "/api/v1/admin/**"
                ).hasRole("ADMIN")

                // Everything else requires JWT
                .anyRequest().authenticated()
            )

            // =========================
            // JWT RESOURCE SERVER
            // =========================
            .oauth2ResourceServer(oauth ->
                oauth.jwt(jwt -> {})
            );

        return http.build();
    }

    // =========================
    // AUTHENTICATION MANAGER
    // =========================

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {

        return configuration.getAuthenticationManager();
    }
}
