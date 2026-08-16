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

    /**
     * Mencari file account TERBARU milik user aplikasi.
     *
     * Contoh username:
     *
     * regis1
     *
     * akan mencari:
     *
     * user_regis1_*.txt
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
                            + directory.toAbsolutePath()
            );
        }

        if (!Files.isDirectory(directory)) {

            throw new IllegalStateException(
                    "Account path is not a directory: "
                            + directory.toAbsolutePath()
            );
        }

        String safeUsername =
                username.trim()
                        .replaceAll(
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
                                                    + " in "
                                                    + directory
                                                            .toAbsolutePath()
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