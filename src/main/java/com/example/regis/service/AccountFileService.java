package com.example.regis.service;

import com.example.regis.config.WorkerProperties;

import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Comparator;

@Service
public class AccountFileService {

    private final WorkerProperties properties;


    public AccountFileService(
            WorkerProperties properties
    ) {

        this.properties =
                properties;
    }


    /*
     * ============================================================
     * GET LATEST ACCOUNT FILE
     * ============================================================
     *
     * Digunakan HANYA saat Job dibuat.
     *
     * Setelah Job dibuat, hasil Path ini disimpan
     * ke dalam object Job.
     */

    public Path getLatestAccountFile(
            String userId
    ) {

        if (
                userId == null ||
                userId.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "User ID cannot be empty"
            );
        }


        String safeUserId =
                userId.replaceAll(
                        "[^a-zA-Z0-9_-]",
                        "_"
                );


        String accountsDir =
                properties.getAccountsDir();


        if (
                accountsDir == null ||
                accountsDir.isBlank()
        ) {

            throw new IllegalStateException(
                    "worker.accounts-dir is not configured"
            );
        }


        Path directory =
                Path.of(accountsDir);


        if (
                !Files.exists(directory)
        ) {

            throw new IllegalStateException(
                    "Accounts directory does not exist: "
                            + directory
            );
        }


        if (
                !Files.isDirectory(directory)
        ) {

            throw new IllegalStateException(
                    "Accounts path is not a directory: "
                            + directory
            );
        }


        try {

            return Files.list(directory)

                    /*
                     * Hanya file biasa.
                     */
                    .filter(
                            Files::isRegularFile
                    )

                    /*
                     * Hanya file milik user.
                     *
                     * Contoh:
                     *
                     * user_1_20260816_020001_a81f23c4.txt
                     */
                    .filter(
                            path ->
                                    path.getFileName()
                                            .toString()
                                            .startsWith(
                                                    "user_"
                                                            + safeUserId
                                                            + "_"
                                            )
                    )

                    /*
                     * Hanya TXT.
                     */
                    .filter(
                            path ->
                                    path.getFileName()
                                            .toString()
                                            .endsWith(
                                                    ".txt"
                                            )
                    )

                    /*
                     * File terbaru.
                     */
                    .max(
                            Comparator.comparing(
                                    this::lastModified
                            )
                    )

                    .orElseThrow(
                            () ->
                                    new IllegalStateException(
                                            "No account file found for user: "
                                                    + userId
                                    )
                    );

        } catch (
                IllegalStateException e
        ) {

            throw e;

        } catch (
                Exception e
        ) {

            throw new IllegalStateException(
                    "Failed to find account file for user: "
                            + userId,
                    e
            );
        }
    }


    /*
     * ============================================================
     * LAST MODIFIED
     * ============================================================
     */

    private java.nio.file.attribute.FileTime lastModified(
            Path path
    ) {

        try {

            return Files.getLastModifiedTime(
                    path
            );

        } catch (Exception e) {

            return java.nio.file.attribute.FileTime.fromMillis(
                    0
            );
        }
    }
}