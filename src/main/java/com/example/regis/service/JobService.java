package com.example.regis.service;

import com.example.regis.config.WorkerProperties;
import com.example.regis.dto.CreateJobRequest;
import com.example.regis.dto.JobResponse;
import com.example.regis.model.Job;
import com.example.regis.model.JobStatus;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JobService {

    private final Map<String, Job> jobs =
            new ConcurrentHashMap<>();

    private final WorkerProperties properties;
    private final PythonWorkerService worker;

    public JobService(
            WorkerProperties properties,
            PythonWorkerService worker
    ) {
        this.properties = properties;
        this.worker = worker;
    }

    /**
     * Create and start a new job.
     */
    public JobResponse create(CreateJobRequest request) {

        String mode =
                request.mode() == null
                        ? "account-per-link"
                        : request.mode();

        if (!Set.of(
                "account-per-link",
                "link-per-account",
                "one-to-one",
                "repeat-accounts"
        ).contains(mode)) {

            throw new IllegalArgumentException(
                    "Invalid mode"
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

        if (concurrency < 1 ||
                concurrency > properties.getMaxConcurrency()) {

            throw new IllegalArgumentException(
                    "Concurrency exceeds limit"
            );
        }

        if (timeout < 1 ||
                timeout > properties.getMaxTimeoutSeconds()) {

            throw new IllegalArgumentException(
                    "Timeout exceeds limit"
            );
        }

        String jobId =
                UUID.randomUUID().toString();

        Job job =
                new Job(
                        jobId,
                        mode,
                        concurrency,
                        timeout
                );

        job.setStatus(JobStatus.QUEUED);
        job.setProgress(0);
        job.setMessage("Job queued");

        jobs.put(jobId, job);

        worker.execute(job);

        return get(jobId);
    }

    /**
     * Get a job by ID.
     */
    public JobResponse get(String jobId) {

        Job job = require(jobId);

        return toResponse(job);
    }

    /**
     * Get the latest/current job.
     *
     * Used by:
     * GET /api/v1/jobs/status
     */
    public JobResponse getCurrent() {

        Job latest = jobs.values()
                .stream()
                .max(
                        java.util.Comparator.comparing(
                                Job::getCreatedAt
                        )
                )
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "No job found"
                        )
                );

        return toResponse(latest);
    }

    /**
     * Cancel the current/latest job.
     *
     * Used by:
     * POST /api/v1/jobs/cancel
     */
    public void cancelCurrent(String reason) {

        Job latest = jobs.values()
                .stream()
                .max(
                        java.util.Comparator.comparing(
                                Job::getCreatedAt
                        )
                )
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "No job found"
                        )
                );

        cancelJob(latest, reason);
    }

    /**
     * Cancel job by ID.
     */
    public void cancel(String jobId) {

        Job job = require(jobId);

        cancelJob(job, "Job cancelled by user");
    }

    private void cancelJob(
            Job job,
            String reason
    ) {

        Process process = job.getProcess();

        if (job.getStatus() == JobStatus.COMPLETED ||
                job.getStatus() == JobStatus.FAILED ||
                job.getStatus() == JobStatus.CANCELLED) {

            return;
        }

        job.setStatus(JobStatus.CANCELLED);

        job.setMessage(
                reason == null || reason.isBlank()
                        ? "Job cancelled"
                        : reason
        );

        if (process != null && process.isAlive()) {

            process.destroy();

            try {

                if (!process.waitFor(
                        3,
                        java.util.concurrent.TimeUnit.SECONDS
                )) {

                    process.destroyForcibly();
                }

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                process.destroyForcibly();
            }
        }

        job.setCompletedAt(Instant.now());
    }

    private Job require(String jobId) {

        Job job = jobs.get(jobId);

        if (job == null) {

            throw new IllegalArgumentException(
                    "Job not found: " + jobId
            );
        }

        return job;
    }

    private JobResponse toResponse(Job job) {

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
