package com.example.regis.dto;

import jakarta.validation.constraints.NotBlank;

public record AccountRequest(

        @NotBlank
        String userId,

        @NotBlank
        String password,

        @NotBlank
        String fullName,

        @NotBlank
        String bankType,

        @NotBlank
        String accountNumber

) {
}