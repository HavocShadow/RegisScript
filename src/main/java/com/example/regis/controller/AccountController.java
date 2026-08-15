package com.example.regis.controller;

import com.example.regis.dto.AccountRequest;
import com.example.regis.dto.AccountResponse;
import com.example.regis.dto.BulkAccountRequest;
import com.example.regis.service.AccountService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@CrossOrigin(origins = "${app.cors-origin}")
public class AccountController {

    private final AccountService accountService;


    public AccountController(
            AccountService accountService
    ) {

        this.accountService =
                accountService;
    }


    @PostMapping
    public ResponseEntity<AccountResponse> add(
            @Valid
            @RequestBody
            AccountRequest request
    ) {

        return ResponseEntity.ok(
                accountService.add(
                        request
                )
        );
    }


    @PostMapping("/bulk")
    public ResponseEntity<List<AccountResponse>> addBulk(
            @Valid
            @RequestBody
            BulkAccountRequest request
    ) {

        return ResponseEntity.ok(
                accountService.addBulk(
                        request
                )
        );
    }
}