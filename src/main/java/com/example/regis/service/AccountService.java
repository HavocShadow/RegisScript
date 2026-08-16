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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AccountService {

    private final WorkerProperties properties;

    /*
     * Mapping bank.
     */
    private static final Map<String, String> BANK_CODES =
            Map.ofEntries(
                    Map.entry("DANA", "60"),
                    Map.entry("BCA", "37"),
                    Map.entry("BRI", "45"),
                    Map.entry("GOPAY", "59"),
                    Map.entry("MANDIRI", "40"),
                    Map.entry("OVO", "58"),
                    Map.entry("BNI", "38"),
                    Map.entry("LINKAJA", "63"),
                    Map.entry("SHOPEEPAY", "116"),
                    Map.entry("CIMB NIAGA", "57"),
                    Map.entry("SAKUKU", "115"),
                    Map.entry("SEABANK", "120"),
                    Map.entry("ALLO BANK", "124"),
                    Map.entry("BANK ACEH", "126"),
                    Map.entry("ALADIN", "130"),
                    Map.entry("ARTHA GRAHA", "103"),
                    Map.entry("ARTOS", "104"),
                    Map.entry("BCA BLU", "123"),
                    Map.entry("BCA SYARIAH", "47"),
                    Map.entry("BJB", "105"),
                    Map.entry("BUKOPIN", "56"),
                    Map.entry("COMMONWEALTH", "107"),
                    Map.entry("DANAMON", "55"),
                    Map.entry("DBS", "108"),
                    Map.entry("HSBC", "110"),
                    Map.entry("JAGO", "122"),
                    Map.entry("BANK JAKARTA", "109"),
                    Map.entry("JATIM", "111"),
                    Map.entry("MASPION", "42"),
                    Map.entry("MAYBANK", "112"),
                    Map.entry("MEGA", "113"),
                    Map.entry("MEGA SYARIAH", "128"),
                    Map.entry("MESTIKA DHARMA", "132"),
                    Map.entry("MUAMALAT", "51"),
                    Map.entry("NAGARI", "114"),
                    Map.entry("OCBC NISP", "52"),
                    Map.entry("PAN INDONESIA", "46"),
                    Map.entry("PERMATA", "54"),
                    Map.entry("SAQU", "127"),
                    Map.entry("SINARMAS", "43"),
                    Map.entry("SYARIAH INDONESIA", "64"),
                    Map.entry("BTN", "39"),
                    Map.entry("UOB", "53"),
                    Map.entry("HIBANK", "44"),
                    Map.entry("KEB HANA", "129"),
                    Map.entry("NEO BANK", "125"),
                    Map.entry("SMBC", "106"),
                    Map.entry("SUPERBANK", "131")
            );

    private static final DateTimeFormatter FILE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public AccountService(
            WorkerProperties properties
    ) {
        this.properties = properties;
    }

    /*
     * ============================
     * SINGLE ACCOUNT
     * ============================
     */

    public AccountResponse add(
            AccountRequest request
    ) {

        Account account = createAccount(request);

        /*
         * Buat FILE BARU.
         * File lama tidak disentuh.
         */
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

    /*
     * ============================
     * BULK ACCOUNT
     * ============================
     */

    public List<AccountResponse> addBulk(
            BulkAccountRequest request
    ) {

        List<Account> accounts =
                new ArrayList<>();

        /*
         * Validasi seluruh account dahulu.
         */
        for (AccountRequest item : request.accounts()) {

            accounts.add(
                    createAccount(item)
            );
        }

        /*
         * Buat SATU FILE BARU untuk
         * seluruh bulk import.
         *
         * File import sebelumnya
         * TIDAK dihapus.
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
     * SAVE SINGLE
     * ============================
     */

    private void saveAccount(
            Account account
    ) {

        saveAccounts(
                List.of(account)
        );
    }

    /*
     * ============================
     * SAVE ACCOUNTS
     * ============================
     */

    private synchronized void saveAccounts(
            List<Account> accounts
    ) {

        if (
                accounts == null ||
                accounts.isEmpty()
        ) {

            throw new IllegalArgumentException(
                    "Account list cannot be empty"
            );
        }

        /*
         * Ambil userId dari account pertama.
         *
         * Untuk bulk import sebaiknya semua
         * account memang milik user yang sama.
         */
        String userId =
                accounts.get(0)
                        .getUserId()
                        .trim();

        /*
         * Pastikan seluruh account dalam bulk
         * memiliki userId yang sama.
         */
        for (Account account : accounts) {

            if (
                    !userId.equals(
                            account.getUserId().trim()
                    )
            ) {

                throw new IllegalArgumentException(
                        "All accounts in one import "
                                + "must belong to the same user"
                );
            }
        }

        /*
         * Base directory diambil dari
         * worker.accounts.
         *
         * Contoh:
         *
         * worker.accounts=
         * /home/vortexis/Registrar/accounts/account.txt
         *
         * maka directory:
         *
         * /home/vortexis/Registrar/accounts
         */
        Path basePath =
                Path.of(
                        properties.getAccounts()
                );

        Path directory =
                basePath.getParent();

        if (directory == null) {

            directory =
                    Path.of(".");
        }

        try {

            Files.createDirectories(
                    directory
            );

            /*
             * Sanitasi userId agar tidak bisa
             * membuat path aneh.
             */
            String safeUserId =
                    userId.replaceAll(
                            "[^a-zA-Z0-9_-]",
                            "_"
                    );

            /*
             * Timestamp.
             */
            String timestamp =
                    LocalDateTime.now()
                            .format(
                                    FILE_DATE_FORMAT
                            );

            /*
             * UUID agar nama file selalu unik.
             */
            String uniqueId =
                    UUID.randomUUID()
                            .toString()
                            .substring(
                                    0,
                                    8
                            );

            /*
             * Contoh:
             *
             * user_regis1_20260815_233001_a81f23c4.txt
             */
            String fileName =
                    "user_"
                            + safeUserId
                            + "_"
                            + timestamp
                            + "_"
                            + uniqueId
                            + ".txt";

            Path file =
                    directory.resolve(
                            fileName
                    );

            /*
             * Convert account → lines.
             */
            List<String> lines =
                    accounts
                            .stream()
                            .map(
                                    Account::toAccountFileLine
                            )
                            .toList();

            /*
             * CREATE_NEW sangat penting.
             *
             * Kalau file ternyata sudah ada,
             * Java akan gagal daripada
             * menimpa file tersebut.
             */
            Files.write(
                    file,
                    lines,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW
            );

            System.out.println(
                    "[ACCOUNT FILE CREATED] "
                            + file.toAbsolutePath()
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to create account file",
                    e
            );
        }
    }
}