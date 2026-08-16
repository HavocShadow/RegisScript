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

import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JobService {

    private final WorkerProperties properties;

    private final PythonWorkerService worker;

    private final UserRepository userRepository;


    /*
     * Semua job yang sedang/baru dibuat.
     *
     * Key = jobId
     */
    private final Map<String, Job> jobs =
            new ConcurrentHashMap<>();


    public JobService(
            WorkerProperties properties,
            PythonWorkerService worker,
            UserRepository userRepository
    ) {

        this.properties = properties;

        this.worker = worker;

        this.userRepository = userRepository;
    }


    /*
     * ============================================================
     * CREATE / RUN JOB
     * ============================================================
     */
    public JobResponse create(
            CreateJobRequest request
    ) {

        /*
         * Ambil user dari JWT.
         *
         * Jangan mengambil owner dari request frontend.
         */
        User user =
                getAuthenticatedUser();


        String mode =
                request.mode() == null ||
                request.mode().isBlank()
                        ? "account-per-link"
                        : request.mode()
                                .trim();


        mode = mode.toLowerCase();


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


        int concurrency =
                request.concurrency() == null
                        ? 10
                        : request.concurrency();


        int timeout =
                request.timeout() == null
                        ? 15
                        : request.timeout();


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


        if (timeout < 1) {

            throw new IllegalArgumentException(
                    "Timeout must be at least 1 minute"
            );
        }


        if (
                timeout >
                        properties.getMaxTimeoutSeconds()
        ) {

            throw new IllegalArgumentException(
                    "Timeout exceeds limit: "
                            + properties.getMaxTimeoutSeconds()
                            + " seconds"
            );
        }


        /*
         * Pastikan user mempunyai file account.
         *
         * Karena Python nanti akan membaca file ini.
         */
        String ownerUserId =
                user.getId().toString();


        /*
         * Buat job.
         */
        String jobId =
                UUID.randomUUID().toString();


        Job job =
                new Job(
                        jobId,
                        mode,
                        concurrency,
                        timeout,
                        ownerUserId
                );


        /*
         * Simpan sebelum worker dijalankan.
         */
        jobs.put(
                jobId,
                job
        );


        /*
         * Jalankan Python secara asynchronous.
         */
        worker.execute(job);


        return get(
                jobId
        );
    }


    /*
     * ============================================================
     * GET JOB BY ID
     * ============================================================
     */
    public JobResponse get(
            String jobId
    ) {

        Job job =
                requireOwnedJob(jobId);


        return toResponse(job);
    }


    /*
     * ============================================================
     * GET CURRENT JOB
     * ============================================================
     *
     * Digunakan oleh:
     *
     * GET /api/v1/jobs/status
     */
    public JobResponse getCurrent() {

        User user =
                getAuthenticatedUser();


        String ownerUserId =
                user.getId().toString();


        return jobs.values()
                .stream()

                /*
                 * Hanya job milik user tersebut.
                 */
                .filter(
                        job ->
                                job.getOwnerUserId()
                                        .equals(ownerUserId)
                )

                /*
                 * Ambil job terbaru.
                 */
                .max(
                        Comparator.comparing(
                                Job::getCreatedAt
                        )
                )

                .map(this::toResponse)

                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "No job found"
                                )
                );
    }


    /*
     * ============================================================
     * CANCEL CURRENT JOB
     * ============================================================
     */
    public void cancelCurrent(
            String reason
    ) {

        User user =
                getAuthenticatedUser();


        String ownerUserId =
                user.getId().toString();


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


    /*
     * ============================================================
     * CANCEL SPECIFIC JOB
     * ============================================================
     */
    public void cancel(
            String jobId
    ) {

        Job job =
                requireOwnedJob(jobId);


        cancelJob(
                job,
                "Cancelled by user"
        );
    }


    /*
     * ============================================================
     * INTERNAL CANCEL
     * ============================================================
     */
    private void cancelJob(
            Job job,
            String reason
    ) {

        JobStatus status =
                job.getStatus();


        /*
         * Jangan melakukan cancel terhadap
         * job yang sudah selesai.
         */
        if (
                status == JobStatus.COMPLETED ||
                status == JobStatus.FAILED ||
                status == JobStatus.CANCELLED
        ) {

            return;
        }


        /*
         * Tandai dahulu sebagai CANCELLED.
         */
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


        /*
         * Hentikan Python process.
         */
        if (
                process != null &&
                process.isAlive()
        ) {

            process.destroy();


            try {

                if (
                        !process.waitFor(
                                3,
                                java.util.concurrent.TimeUnit.SECONDS
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


    /*
     * ============================================================
     * REQUIRE OWNED JOB
     * ============================================================
     */
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
                user.getId().toString();


        /*
         * SECURITY:
         *
         * User A tidak boleh melihat
         * Job milik User B.
         */
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


    /*
     * ============================================================
     * AUTHENTICATED USER
     * ============================================================
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
     * ============================================================
     * RESPONSE
     * ============================================================
     */
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