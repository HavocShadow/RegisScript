package com.example.regis.service;

import com.example.regis.config.WorkerProperties;
import com.example.regis.dto.AccountRequest;
import com.example.regis.dto.AccountResponse;
import com.example.regis.dto.BulkAccountRequest;
import com.example.regis.model.Account;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AccountService {

    private final WorkerProperties properties;


    /*
     * Mapping bank.
     *
     * Tambahkan bank lainnya di sini.
     */

    private static final Map<String, String> BANK_CODES =
            Map.of(
                    "LINKAJA", "63"
            );


    public AccountService(
            WorkerProperties properties
    ) {

        this.properties = properties;
    }


    public AccountResponse add(
            AccountRequest request
    ) {

        Account account =
                createAccount(request);


        saveAccount(account);


        return new AccountResponse(

                account.getUserId(),

                account.getFullName(),

                account.getBankType(),

                account.getBankCode(),

                account.getAccountNumber(),

                true
        );
    }


    public List<AccountResponse> addBulk(
            BulkAccountRequest request
    ) {

        List<Account> accounts =
                new ArrayList<>();


        /*
         * Validasi SEMUA account dahulu.
         *
         * Kalau satu invalid,
         * jangan tulis sebagian.
         */

        for (
                AccountRequest item :
                request.accounts()
        ) {

            accounts.add(
                    createAccount(item)
            );
        }


        /*
         * Setelah semuanya valid,
         * baru tulis ke accounts.txt.
         */

        saveAccounts(accounts);


        return accounts
                .stream()
                .map(
                        account ->
                                new AccountResponse(

                                        account.getUserId(),

                                        account.getFullName(),

                                        account.getBankType(),

                                        account.getBankCode(),

                                        account.getAccountNumber(),

                                        true
                                )
                )
                .toList();
    }


    private Account createAccount(
            AccountRequest request
    ) {

        String bankType =
                request.bankType()
                        .trim()
                        .toUpperCase();


        String bankCode =
                BANK_CODES.get(
                        bankType
                );


        if (bankCode == null) {

            throw new IllegalArgumentException(
                    "Unsupported bank type: "
                            + bankType
            );
        }


        return new Account(

                request.userId().trim(),

                request.password().trim(),

                request.fullName().trim(),

                bankType,

                bankCode,

                request.accountNumber().trim()
        );
    }


    private synchronized void saveAccount(
            Account account
    ) {

        saveAccounts(
                List.of(account)
        );
    }


    private synchronized void saveAccounts(
            List<Account> accounts
    ) {

        Path path =
                Path.of(
                        properties.getAccounts()
                );


        try {

            if (path.getParent() != null) {

                Files.createDirectories(
                        path.getParent()
                );
            }


            List<String> lines =
                    accounts
                            .stream()
                            .map(
                                    Account::toAccountFileLine
                            )
                            .toList();


            Files.write(

                    path,

                    lines,

                    StandardCharsets.UTF_8,

                    StandardOpenOption.CREATE,

                    StandardOpenOption.APPEND
            );


        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to write accounts.txt",
                    e
            );
        }
    }
}