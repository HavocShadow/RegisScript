package com.example.regis.controller;

import com.example.regis.dto.LoginRequest;
import com.example.regis.dto.LoginResponse;
import com.example.regis.model.User;
import com.example.regis.repository.UserRepository;
import com.example.regis.security.JwtService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            JwtService jwtService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.username(),
                                request.password()
                        )
                );

        User user =
                userRepository
                        .findByUsername(request.username())
                        .orElseThrow();

        String token =
                jwtService.generateToken(
                        user.getUsername(),
                        user.getRole()
                );

        return ResponseEntity.ok(
                new LoginResponse(
                        token,
                        "Bearer",
                        900,
                        new LoginResponse.UserInfo(
                                user.getUsername(),
                                user.getRole()
                        )
                )
        );
    }
}