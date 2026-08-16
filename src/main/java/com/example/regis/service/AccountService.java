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
        Map.ofEntries(

                Map.entry("DANA", "60"),
                Map.entry("BANK BCA", "37"),
                Map.entry("BANK BRI", "45"),
                Map.entry("GOPAY", "59"),
                Map.entry("BANK MANDIRI", "40"),
                Map.entry("OVO", "58"),
                Map.entry("BANK BNI", "38"),
                Map.entry("LINKAJA", "63"),
                Map.entry("SHOPEEPAY", "116"),
                Map.entry("BANK CIMB NIAGA", "57"),
                Map.entry("SAKUKU", "115"),
                Map.entry("SEABANK", "120"),
                Map.entry("ALLO BANK", "124"),
                Map.entry("BANK ACEH", "126"),
                Map.entry("BANK ALADIN", "130"),
                Map.entry("BANK ARTHA GRAHA", "103"),
                Map.entry("BANK ARTOS", "104"),
                Map.entry("BANK BCA BLU", "123"),
                Map.entry("BANK BCA SYARIAH", "47"),
                Map.entry("BANK BJB", "105"),
                Map.entry("BANK BUKOPIN", "56"),
                Map.entry("BANK COMMONWEALTH", "107"),
                Map.entry("BANK DANAMON", "55"),
                Map.entry("BANK DBS", "108"),
                Map.entry("BANK HSBC", "110"),
                Map.entry("BANK JAGO", "122"),
                Map.entry("BANK JAKARTA", "109"),
                Map.entry("BANK JATIM", "111"),
                Map.entry("BANK MASPION", "42"),
                Map.entry("BANK MAYBANK", "112"),
                Map.entry("BANK MEGA", "113"),
                Map.entry("BANK MEGA SYARIAH", "128"),
                Map.entry("BANK MESTIKA DHARMA", "132"),
                Map.entry("BANK MUAMALAT INDONESIA", "51"),
                Map.entry("BANK NAGARI", "114"),
                Map.entry("BANK OCBC NISP", "52"),
                Map.entry("BANK PAN INDONESIA", "46"),
                Map.entry("BANK PERMATA", "54"),
                Map.entry("BANK SAQU", "127"),
                Map.entry("BANK SINARMAS", "43"),
                Map.entry("BANK SYARIAH INDONESIA", "64"),
                Map.entry("BANK TABUNGAN NEGARA", "39"),
                Map.entry("BANK UOB", "53"),
                Map.entry("HIBANK", "44"),
                Map.entry("KEB HANA", "129"),
                Map.entry("NEO BANK", "125"),
                Map.entry("SMBC", "106"),
                Map.entry("SUPERBANK", "131")
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
