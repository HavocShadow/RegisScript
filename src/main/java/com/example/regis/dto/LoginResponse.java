package com.example.regis.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserInfo user
) {

    public record UserInfo(
            String username,
            String role
    ) {}
}