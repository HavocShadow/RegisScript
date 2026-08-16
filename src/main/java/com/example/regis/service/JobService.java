package com.example.regis.service;

import com.example.regis.config.WorkerProperties;
import com.example.regis.dto.CreateJobRequest;
import com.example.regis.dto.JobResponse;
import com.example.regis.model.Job;
import com.example.regis.model.JobStatus;
import com.example.regis.model.User;
import com.example.regis.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class JobService {

    private final WorkerProperties properties;

    private final PythonWorkerService worker;

    private final UserRepository userRepository;

    private final AccountFileService accountFileService;


    private final Map<String, Job> jobs =
            new ConcurrentHashMap<>();


    public JobService(
            WorkerProperties properties,
            PythonWorkerService worker,
            UserRepository userRepository,
            AccountFileService accountFileService
    ) {

        this.properties =
                properties;

        this.worker =
                worker;

        this.userRepository =
                userRepository;

        this.accountFileService =
                accountFileService;
    }


    // ============================================================
    // CREATE JOB
    // ============================================================

    public JobResponse create(
            CreateJobRequest request
    ) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Job request cannot be null"
            );
        }


        User user =
                getAuthenticatedUser();


        // ========================================================
        // MODE
        // ========================================================

        String mode =
                request.mode() == null ||
                request.mode().isBlank()

                        ? "account-per-link"

                        : request.mode()
                                .trim()
                                .toLowerCase();


        if (
                !Set.of(
                        "account-per-link",
                        "link-per-account",
                        "one-to-one",
                        "repeat-accounts"
                ).contains(mode)
        ) {

            throw new IllegalArgumentException(
                    "Invalid mode: " + mode
            );
        }


        // ========================================================
        // CONCURRENCY
        // ========================================================

        int concurrency =
                request.concurrency() == null

                        ? 10

                        : request.concurrency();


        if (concurrency < 1) {

            throw new IllegalArgumentException(
                    "Concurrency must be at least 1"
            );
        }


        if (
                concurrency >
                        properties.getMaxConcurrency()
        ) {

            throw new IllegalArgumentException(
                    "Concurrency exceeds limit: "
                            + properties.getMaxConcurrency()
            );
        }


        // ========================================================
        // TIMEOUT
        // ========================================================

        int timeout =
                request.timeout() == null

                        ? 15

                        : request.timeout();


        if (timeout < 1) {

            throw new IllegalArgumentException(
                    "Timeout must be at least 1 minute"
            );
        }


        if (
                timeout >
                        properties.getMaxTimeoutMinutes()
        ) {

            throw new IllegalArgumentException(
                    "Timeout exceeds limit: "
                            + properties.getMaxTimeoutMinutes()
                            + " minute(s)"
            );
        }


        // ========================================================
        // OWNER
        // ========================================================

        String ownerUserId =
                user.getId()
                        .toString();


        // ========================================================
        // ACCOUNT FILE SNAPSHOT
        // ========================================================
        //
        // Ambil file ACCOUNT TERBARU milik user.
        //
        // File ini kemudian disimpan langsung
        // ke dalam Job.
        //
        // Worker TIDAK akan mencari file lagi
        // berdasarkan userId.
        //

        Path accountFile =
                accountFileService
                        .getLatestAccountFile(
                                ownerUserId
                        );


        if (
                accountFile == null
        ) {

            throw new IllegalStateException(
                    "No account file found for user"
            );
        }


        if (
                !Files.exists(accountFile)
        ) {

            throw new IllegalStateException(
                    "Account file does not exist: "
                            + accountFile
            );
        }


        if (
                !Files.isRegularFile(accountFile)
        ) {

            throw new IllegalStateException(
                    "Account path is not a regular file: "
                            + accountFile
            );
        }


        try {

            if (
                    Files.size(accountFile) == 0
            ) {

                throw new IllegalStateException(
                        "Account file is empty: "
                                + accountFile
                );
            }

        } catch (
                java.io.IOException e
        ) {

            throw new IllegalStateException(
                    "Failed to inspect account file: "
                            + accountFile,
                    e
            );
        }


        // ========================================================
        // ABSOLUTE PATH
        // ========================================================

        String accountFilePath =
                accountFile
                        .toAbsolutePath()
                        .normalize()
                        .toString();


        // ========================================================
        // JOB ID
        // ========================================================

        String jobId =
                UUID.randomUUID()
                        .toString();


        // ========================================================
        // CREATE JOB
        // ========================================================

        Job job =
                new Job(
                        jobId,
                        ownerUserId,
                        accountFilePath,
                        mode,
                        concurrency,
                        timeout
                );


        // ========================================================
        // STORE JOB
        // ========================================================

        jobs.put(
                jobId,
                job
        );


        // ========================================================
        // START PYTHON WORKER
        // ========================================================

        worker.execute(
                job
        );


        // ========================================================
        // RESPONSE
        // ========================================================

        return toResponse(
                job
        );
    }


    // ============================================================
    // GET
    // ============================================================

    public JobResponse get(
            String jobId
    ) {

        Job job =
                requireOwnedJob(
                        jobId
                );


        return toResponse(
                job
        );
    }


    // ============================================================
    // CURRENT
    // ============================================================

    public JobResponse getCurrent() {

        User user =
                getAuthenticatedUser();


        String ownerUserId =
                user.getId()
                        .toString();


        return jobs.values()
                .stream()

                .filter(
                        job ->
                                job.getOwnerUserId()
                                        .equals(ownerUserId)
                )

                .max(
                        Comparator.comparing(
                                Job::getCreatedAt
                        )
                )

                .map(
                        this::toResponse
                )

                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "No job found"
                                )
                );
    }


    // ============================================================
    // CANCEL CURRENT
    // ============================================================

    public void cancelCurrent(
            String reason
    ) {

        User user =
                getAuthenticatedUser();


        String ownerUserId =
                user.getId()
                        .toString();


        Job job =
                jobs.values()
                        .stream()

                        .filter(
                                item ->
                                        item.getOwnerUserId()
                                                .equals(ownerUserId)
                        )

                        .max(
                                Comparator.comparing(
                                        Job::getCreatedAt
                                )
                        )

                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "No job found"
                                        )
                        );


        cancelJob(
                job,
                reason
        );
    }


    // ============================================================
    // CANCEL
    // ============================================================

    public void cancel(
            String jobId
    ) {

        Job job =
                requireOwnedJob(
                        jobId
                );


        cancelJob(
                job,
                "Cancelled by user"
        );
    }


    // ============================================================
    // INTERNAL CANCEL
    // ============================================================

    private void cancelJob(
            Job job,
            String reason
    ) {

        JobStatus status =
                job.getStatus();


        if (
                status == JobStatus.COMPLETED ||
                status == JobStatus.FAILED ||
                status == JobStatus.CANCELLED
        ) {

            return;
        }


        job.setStatus(
                JobStatus.CANCELLED
        );


        job.setMessage(
                reason == null ||
                reason.isBlank()

                        ? "Job cancelled"

                        : reason
        );


        Process process =
                job.getProcess();


        if (
                process != null &&
                process.isAlive()
        ) {

            process.destroy();


            try {

                if (
                        !process.waitFor(
                                3,
                                TimeUnit.SECONDS
                        )
                ) {

                    process.destroyForcibly();
                }

            } catch (
                    InterruptedException e
            ) {

                Thread.currentThread()
                        .interrupt();

                process.destroyForcibly();
            }
        }


        job.setCompletedAt(
                Instant.now()
        );
    }


    // ============================================================
    // REQUIRE OWNED JOB
    // ============================================================

    private Job requireOwnedJob(
            String jobId
    ) {

        Job job =
                jobs.get(jobId);


        if (job == null) {

            throw new IllegalArgumentException(
                    "Job not found"
            );
        }


        User user =
                getAuthenticatedUser();


        String ownerUserId =
                user.getId()
                        .toString();


        if (
                !job.getOwnerUserId()
                        .equals(ownerUserId)
        ) {

            throw new IllegalArgumentException(
                    "Job not found"
            );
        }


        return job;
    }


    // ============================================================
    // AUTHENTICATED USER
    // ============================================================

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


    // ============================================================
    // RESPONSE
    // ============================================================

    private JobResponse toResponse(
            Job job
    ) {

        return new JobResponse(

                job.getJobId(),

                job.getMode(),

                job.getConcurrency(),

                job.getTimeout(),

                job.getStatus(),

                job.getProgress(),

                job.getMessage(),

                job.getCreatedAt(),

                job.getStartedAt(),

                job.getCompletedAt(),

                job.getError()
        );
    }
}