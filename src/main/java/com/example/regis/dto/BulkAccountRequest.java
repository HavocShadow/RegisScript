package com.example.regis.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BulkAccountRequest(

        @NotEmpty
        List<@Valid AccountRequest> accounts

) {
}