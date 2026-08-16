package com.example.regis.model;

import java.time.Instant;

public class Job {

    private final String jobId;

    /**
     * User yang membuat / menjalankan job.
     * Digunakan untuk menentukan account file milik user tersebut.
     */
    private final String ownerUserId;

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
            String mode,
            int concurrency,
            int timeout
    ) {

        this.jobId = jobId;
        this.ownerUserId = ownerUserId;
        this.mode = mode;
        this.concurrency = concurrency;
        this.timeout = timeout;

        this.status = JobStatus.QUEUED;
        this.progress = 0;
        this.message = "Job queued";
        this.error = null;

        this.createdAt = Instant.now();
        this.startedAt = null;
        this.completedAt = null;

        this.process = null;
    }


    // =========================================================
    // JOB ID
    // =========================================================

    public String getJobId() {

        return jobId;
    }


    // =========================================================
    // OWNER USER
    // =========================================================

    public String getOwnerUserId() {

        return ownerUserId;
    }


    // =========================================================
    // JOB CONFIGURATION
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

        this.status = status;
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

        /*
         * Jangan biarkan progress keluar
         * dari range 0 - 100.
         */

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

        this.message = message;
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

        this.error = error;
    }


    // =========================================================
    // CREATED AT
    // =========================================================

    public Instant getCreatedAt() {

        return createdAt;
    }


    public void setCreatedAt(
            Instant createdAt
    ) {

        this.createdAt = createdAt;
    }


    // =========================================================
    // STARTED AT
    // =========================================================

    public Instant getStartedAt() {

        return startedAt;
    }


    public void setStartedAt(
            Instant startedAt
    ) {

        this.startedAt = startedAt;
    }


    // =========================================================
    // COMPLETED AT
    // =========================================================

    public Instant getCompletedAt() {

        return completedAt;
    }


    public void setCompletedAt(
            Instant completedAt
    ) {

        this.completedAt = completedAt;
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

        this.process = process;
    }
}