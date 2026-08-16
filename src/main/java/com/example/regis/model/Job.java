package com.example.regis.service;

import com.example.regis.config.WorkerProperties;
import com.example.regis.dto.AccountRequest;
import com.example.regis.dto.AccountResponse;
import com.example.regis.dto.BulkAccountRequest;
import com.example.regis.model.Account;
import com.example.regis.model.User;
import com.example.regis.repository.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final UserRepository userRepository;

    /*
     * Bank code berdasarkan value
     * yang diberikan oleh frontend/source Anda.
     */
    private static final Map<String, String> BANK_CODES =
            Map.ofEntries(

                    Map.entry("DANA", "60"),
                    Map.entry("BCA", "37"),
                    Map.entry("BANK BCA", "37"),

                    Map.entry("BRI", "45"),
                    Map.entry("BANK BRI", "45"),

                    Map.entry("GOPAY", "59"),
                    Map.entry("GO PAY", "59"),

                    Map.entry("MANDIRI", "40"),
                    Map.entry("BANK MANDIRI", "40"),

                    Map.entry("OVO", "58"),

                    Map.entry("BNI", "38"),
                    Map.entry("BANK BNI", "38"),

                    Map.entry("LINKAJA", "63"),

                    Map.entry("SHOPEEPAY", "116"),

                    Map.entry("CIMB NIAGA", "57"),
                    Map.entry("BANK CIMB NIAGA", "57"),

                    Map.entry("SAKUKU", "115"),

                    Map.entry("SEABANK", "120"),

                    Map.entry("ALLO BANK", "124"),
                    Map.entry("ALLOBANK", "124"),

                    Map.entry("BANK ACEH", "126"),

                    Map.entry("BANK ALADIN", "130"),
                    Map.entry("ALADIN", "130"),

                    Map.entry("BANK ARTHA GRAHA", "103"),

                    Map.entry("BANK ARTOS", "104"),
                    Map.entry("ARTOS", "104"),

                    Map.entry("BANK BCA BLU", "123"),
                    Map.entry("BLU", "123"),

                    Map.entry("BANK BCA SYARIAH", "47"),
                    Map.entry("BCA SYARIAH", "47"),

                    Map.entry("BANK BJB", "105"),
                    Map.entry("BJB", "105"),

                    Map.entry("BANK BUKOPIN", "56"),
                    Map.entry("BUKOPIN", "56"),

                    Map.entry("BANK COMMONWEALTH", "107"),

                    Map.entry("BANK DANAMON", "55"),
                    Map.entry("DANAMON", "55"),

                    Map.entry("BANK DBS", "108"),
                    Map.entry("DBS", "108"),

                    Map.entry("BANK HSBC", "110"),
                    Map.entry("HSBC", "110"),

                    Map.entry("BANK JAGO", "122"),
                    Map.entry("JAGO", "122"),

                    Map.entry("BANK JAKARTA", "109"),

                    Map.entry("BANK JATIM", "111"),
                    Map.entry("JATIM", "111"),

                    Map.entry("BANK MASPION", "42"),
                    Map.entry("MASPION", "42"),

                    Map.entry("BANK MAYBANK", "112"),
                    Map.entry("MAYBANK", "112"),

                    Map.entry("BANK MEGA", "113"),
                    Map.entry("MEGA", "113"),

                    Map.entry("BANK MEGA SYARIAH", "128"),
                    Map.entry("MEGA SYARIAH", "128"),

                    Map.entry("BANK MESTIKA DHARMA", "132"),

                    Map.entry("BANK MUAMALAT INDONESIA", "51"),
                    Map.entry("MUAMALAT", "51"),

                    Map.entry("BANK NAGARI", "114"),
                    Map.entry("NAGARI", "114"),

                    Map.entry("BANK OCBC NISP", "52"),
                    Map.entry("OCBC", "52"),

                    Map.entry("BANK PAN INDONESIA", "46"),
                    Map.entry("PANIN", "46"),

                    Map.entry("BANK PERMATA", "54"),
                    Map.entry("PERMATA", "54"),

                    Map.entry("BANK SAQU", "127"),
                    Map.entry("SAQU", "127"),

                    Map.entry("BANK SINARMAS", "43"),
                    Map.entry("SINARMAS", "43"),

                    Map.entry("BANK SYARIAH INDONESIA", "64"),
                    Map.entry("BSI", "64"),

                    Map.entry("BANK TABUNGAN NEGARA", "39"),
                    Map.entry("BTN", "39"),

                    Map.entry("BANK UOB", "53"),
                    Map.entry("UOB", "53"),

                    Map.entry("HIBANK", "44"),

                    Map.entry("KEB HANA", "129"),

                    Map.entry("NEO BANK", "125"),
                    Map.entry("NEOBANK", "125"),

                    Map.entry("SMBC", "106"),

                    Map.entry("SUPERBANK", "131")
            );

    public AccountService(
            WorkerProperties properties,
            UserRepository userRepository
    ) {
        this.properties = properties;
        this.userRepository = userRepository;
    }

    /*
     * ============================
     * ADD SINGLE ACCOUNT
     * ============================
     */
    public AccountResponse add(AccountRequest request) {

        User user = getAuthenticatedUser();

        Account account = createAccount(request);

        /*
         * Single add:
         * baca file existing user,
         * lalu tambahkan account baru.
         */
        appendAccount(
                user.getId().toString(),
                account
        );

        return toResponse(account);
    }

    /*
     * ============================
     * BULK IMPORT
     * ============================
     */
    public List<AccountResponse> addBulk(
            BulkAccountRequest request
    ) {

        User user = getAuthenticatedUser();

        List<Account> accounts =
                new ArrayList<>();

        /*
         * Validasi semua account dahulu.
         */
        for (AccountRequest item : request.accounts()) {

            accounts.add(
                    createAccount(item)
            );
        }

        /*
         * BULK IMPORT = REPLACE TOTAL
         *
         * File lama user ini dihapus/
         * ditimpa dengan data baru.
         */
        replaceUserAccounts(
                user.getId().toString(),
                accounts
        );

        return accounts
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /*
     * ============================
     * CREATE ACCOUNT
     * ============================
     */
    private Account createAccount(
            AccountRequest request
    ) {

        String bankType =
                request.bankType()
                        .trim()
                        .toUpperCase();

        String bankCode =
                BANK_CODES.get(bankType);

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

    /*
     * ============================
     * AUTHENTICATED USER
     * ============================
     */
    private User getAuthenticatedUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (
                authentication == null ||
                !authentication.isAuthenticated()
        ) {

            throw new IllegalStateException(
                    "User is not authenticated"
            );
        }

        String username =
                authentication.getName();

        if (
                username == null ||
                username.isBlank()
        ) {

            throw new IllegalStateException(
                    "Authenticated username is missing"
            );
        }

        return userRepository
                .findByUsername(username)
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Authenticated user not found"
                                )
                );
    }

    /*
     * ============================
     * USER ACCOUNT FILE
     * ============================
     */
    private Path getUserAccountFile(
            String userId
    ) {

        /*
         * Jangan biarkan userId mengandung
         * path traversal.
         */
        String safeUserId =
                userId.replaceAll(
                        "[^a-zA-Z0-9_-]",
                        "_"
                );

        Path directory =
                Path.of(
                        properties.getAccountsDir()
                );

        return directory.resolve(
                safeUserId + ".txt"
        );
    }

    /*
     * ============================
     * APPEND SINGLE ACCOUNT
     * ============================
     */
    private synchronized void appendAccount(
            String userId,
            Account account
    ) {

        Path path =
                getUserAccountFile(userId);

        try {

            if (path.getParent() != null) {

                Files.createDirectories(
                        path.getParent()
                );
            }

            Files.writeString(

                    path,

                    account.toAccountFileLine()
                            + System.lineSeparator(),

                    StandardCharsets.UTF_8,

                    StandardOpenOption.CREATE,

                    StandardOpenOption.APPEND
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to save account",
                    e
            );
        }
    }

    /*
     * ============================
     * REPLACE BULK ACCOUNTS
     * ============================
     */
    private synchronized void replaceUserAccounts(
            String userId,
            List<Account> accounts
    ) {

        Path path =
                getUserAccountFile(userId);

        try {

            if (path.getParent() != null) {

                Files.createDirectories(
                        path.getParent()
                );
            }

            /*
             * Jangan APPEND.
             *
             * TRUNCATE_EXISTING memastikan
             * data lama user diganti.
             */
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

                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to replace user accounts",
                    e
            );
        }
    }

    /*
     * ============================
     * RESPONSE
     * ============================
     */
    private AccountResponse toResponse(
            Account account
    ) {

        return new AccountResponse(

                account.getUserId(),

                account.getFullName(),

                account.getBankType(),

                account.getBankCode(),

                account.getAccountNumber(),

                true
        );
    }
}