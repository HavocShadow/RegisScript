package com.example.regis.service;

import com.example.regis.config.WorkerProperties;

import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Comparator;
import java.util.stream.Stream;

@Service
public class AccountFileService {

    private final WorkerProperties properties;

    public AccountFileService(
            WorkerProperties properties
    ) {
        this.properties = properties;
    }

    /*
     * ============================================================
     * GET LATEST ACCOUNT FILE
     * ============================================================
     *
     * Mencari berdasarkan USERNAME.
     *
     * Contoh:
     *
     * username = toniva00238
     *
     * akan mencari:
     *
     * user_toniva00238_*.txt
     */
    public Path getLatestAccountFile(
            String username
    ) {

        if (
                username == null ||
                username.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Username cannot be empty"
            );
        }

        String accountsDir =
                properties.getAccounts();

        if (
                accountsDir == null ||
                accountsDir.isBlank()
        ) {
            throw new IllegalStateException(
                    "worker.accounts is not configured"
            );
        }

        Path directory =
                Path.of(accountsDir);

        if (!Files.exists(directory)) {

            throw new IllegalStateException(
                    "Account directory does not exist: "
                            + directory
            );
        }

        if (!Files.isDirectory(directory)) {

            throw new IllegalStateException(
                    "Account path is not a directory: "
                            + directory
            );
        }

        String safeUsername =
                username.replaceAll(
                        "[^a-zA-Z0-9_-]",
                        "_"
                );

        String prefix =
                "user_"
                        + safeUsername
                        + "_";

        try (
                Stream<Path> files =
                        Files.list(directory)
        ) {

            return files

                    .filter(
                            Files::isRegularFile
                    )

                    .filter(
                            path ->
                                    path.getFileName()
                                            .toString()
                                            .startsWith(
                                                    prefix
                                            )
                    )

                    .filter(
                            path ->
                                    path.getFileName()
                                            .toString()
                                            .endsWith(
                                                    ".txt"
                                            )
                    )

                    .max(
                            Comparator.comparing(
                                    path -> {
                                        try {

                                            return Files
                                                    .getLastModifiedTime(
                                                            path
                                                    )
                                                    .toMillis();

                                        } catch (Exception e) {

                                            return 0L;
                                        }
                                    }
                            )
                    )

                    .orElseThrow(
                            () ->
                                    new IllegalStateException(
                                            "No account file found for user: "
                                                    + username
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
                            + username,
                    e
            );
        }
    }
}