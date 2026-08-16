package com.example.regis.service;

import com.example.regis.dto.LoginRequest;
import com.example.regis.dto.RegisterRequest;
import com.example.regis.model.User;
import com.example.regis.repository.UserRepository;
import com.example.regis.security.JwtService;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public String login(LoginRequest request) {

        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                );

        Authentication authentication =
                authenticationManager.authenticate(token);

        String username = authentication.getName();

        String role = authentication.getAuthorities()
                .stream()
                .findFirst()
                .map(authority -> authority.getAuthority())
                .orElse("ROLE_USER");

        return jwtService.generateToken(username, role);
    }

    @Transactional
    public User register(RegisterRequest request) {

        String username =
                request.getUsername().trim();

        String email =
                request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException(
                    "Username already exists"
            );
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Email already exists"
            );
        }

        User user = new User();

        user.setUsername(username);
        user.setEmail(email);

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        // Public registration selalu USER
        user.setRole("USER");

        user.setEnabled(true);

        try {

            return userRepository.save(user);

        } catch (DataIntegrityViolationException e) {

            throw new IllegalArgumentException(
                    "Username or email already exists"
            );
        }
    }
}
