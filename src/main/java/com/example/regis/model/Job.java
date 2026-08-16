package com.example.regis.model;

import java.time.Instant;

public class Job {

    private final String jobId;

    /*
     * User yang membuat job.
     */
    private final String ownerUserId;

    /*
     * File account yang secara eksplisit
     * digunakan oleh job ini.
     */
    private final String accountFile;

    private final String mode;

    private final int concurrency;

    private final int timeout;

    private volatile JobStatus status;

    private volatile int progress;

    private volatile String message;

    private volatile String error;

    private volatile Instant createdAt;

    private volatile Instant startedAt;

    private volatile Instant completedAt;

    private volatile Process process;


    public Job(
            String jobId,
            String ownerUserId,
            String accountFile,
            String mode,
            int concurrency,
            int timeout
    ) {

        this.jobId =
                jobId;

        this.ownerUserId =
                ownerUserId;

        this.accountFile =
                accountFile;

        this.mode =
                mode;

        this.concurrency =
                concurrency;

        this.timeout =
                timeout;

        this.status =
                JobStatus.QUEUED;

        this.progress =
                0;

        this.message =
                "Job queued";

        this.error =
                null;

        this.createdAt =
                Instant.now();

        this.startedAt =
                null;

        this.completedAt =
                null;

        this.process =
                null;
    }


    // =========================================================
    // JOB ID
    // =========================================================

    public String getJobId() {

        return jobId;
    }


    // =========================================================
    // OWNER
    // =========================================================

    public String getOwnerUserId() {

        return ownerUserId;
    }


    // =========================================================
    // ACCOUNT FILE
    // =========================================================

    public String getAccountFile() {

        return accountFile;
    }


    // =========================================================
    // CONFIGURATION
    // =========================================================

    public String getMode() {

        return mode;
    }


    public int getConcurrency() {

        return concurrency;
    }


    public int getTimeout() {

        return timeout;
    }


    // =========================================================
    // STATUS
    // =========================================================

    public JobStatus getStatus() {

        return status;
    }


    public void setStatus(
            JobStatus status
    ) {

        this.status =
                status;
    }


    // =========================================================
    // PROGRESS
    // =========================================================

    public int getProgress() {

        return progress;
    }


    public void setProgress(
            int progress
    ) {

        this.progress =
                Math.max(
                        0,
                        Math.min(
                                100,
                                progress
                        )
                );
    }


    // =========================================================
    // MESSAGE
    // =========================================================

    public String getMessage() {

        return message;
    }


    public void setMessage(
            String message
    ) {

        this.message =
                message;
    }


    // =========================================================
    // ERROR
    // =========================================================

    public String getError() {

        return error;
    }


    public void setError(
            String error
    ) {

        this.error =
                error;
    }


    // =========================================================
    // CREATED
    // =========================================================

    public Instant getCreatedAt() {

        return createdAt;
    }


    public void setCreatedAt(
            Instant createdAt
    ) {

        this.createdAt =
                createdAt;
    }


    // =========================================================
    // STARTED
    // =========================================================

    public Instant getStartedAt() {

        return startedAt;
    }


    public void setStartedAt(
            Instant startedAt
    ) {

        this.startedAt =
                startedAt;
    }


    // =========================================================
    // COMPLETED
    // =========================================================

    public Instant getCompletedAt() {

        return completedAt;
    }


    public void setCompletedAt(
            Instant completedAt
    ) {

        this.completedAt =
                completedAt;
    }


    // =========================================================
    // PROCESS
    // =========================================================

    public Process getProcess() {

        return process;
    }


    public void setProcess(
            Process process
    ) {

        this.process =
                process;
    }
}