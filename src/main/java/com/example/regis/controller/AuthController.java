package com.example.regis.controller;

import com.example.regis.dto.LoginRequest;
import com.example.regis.dto.RegisterRequest;
import com.example.regis.model.User;
import com.example.regis.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // =========================
    // REGISTER
    // =========================

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        try {

            User user = authService.register(request);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(
                            Map.of(
                                    "success", true,
                                    "message", "Registration successful",
                                    "user", Map.of(
                                            "id", user.getId(),
                                            "username", user.getUsername(),
                                            "email", user.getEmail()
                                    )
                            )
                    );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(
                            Map.of(
                                    "success", false,
                                    "message", e.getMessage()
                            )
                    );
        }
    }

    // =========================
    // LOGIN
    // =========================

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request
    ) {

        try {

            String accessToken =
                    authService.login(request);

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "message", "Login successful",
                            "accessToken", accessToken,
                            "tokenType", "Bearer"
                    )
            );

        } catch (Exception e) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            Map.of(
                                    "success", false,
                                    "message",
                                    "Invalid username or password"
                            )
                    );
        }
    }
}
