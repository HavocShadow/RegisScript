package com.example.regis.dto;

public record AccountResponse(

        String userId,

        String fullName,

        String bankType,

        String bankCode,

        String accountNumber,

        boolean saved

) {
}