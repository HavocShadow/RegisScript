package com.example.regis.model;

import java.time.Instant;

public class Job {

    private final String jobId;

    /**
     * ID user yang membuat job.
     *
     * Digunakan untuk menentukan:
     * - ownership job
     * - account file milik user
     */
    private final String ownerUserId;

    /**
     * Mode worker.
     */
    private final String mode;

    /**
     * Jumlah worker/concurrency.
     */
    private final int concurrency;

    /**
     * Timeout job.
     *
     * Satuan mengikuti konfigurasi/request
     * yang digunakan oleh aplikasi.
     */
    private final int timeout;

    /**
     * Status job saat ini.
     */
    private volatile JobStatus status;

    /**
     * Progress 0 - 100.
     */
    private volatile int progress;

    /**
     * Pesan status job.
     */
    private volatile String message;

    /**
     * Error jika job gagal.
     */
    private volatile String error;

    /**
     * Waktu job dibuat.
     */
    private volatile Instant createdAt;

    /**
     * Waktu worker mulai dijalankan.
     */
    private volatile Instant startedAt;

    /**
     * Waktu job selesai.
     */
    private volatile Instant completedAt;

    /**
     * Python process yang sedang menjalankan job.
     */
    private volatile Process process;


    /*
     * =========================================================
     * CONSTRUCTOR
     * =========================================================
     */

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


    /*
     * =========================================================
     * JOB ID
     * =========================================================
     */

    public String getJobId() {

        return jobId;
    }


    /*
     * =========================================================
     * OWNER USER ID
     * =========================================================
     */

    public String getOwnerUserId() {

        return ownerUserId;
    }


    /*
     * =========================================================
     * MODE
     * =========================================================
     */

    public String getMode() {

        return mode;
    }


    /*
     * =========================================================
     * CONCURRENCY
     * =========================================================
     */

    public int getConcurrency() {

        return concurrency;
    }


    /*
     * =========================================================
     * TIMEOUT
     * =========================================================
     */

    public int getTimeout() {

        return timeout;
    }


    /*
     * =========================================================
     * STATUS
     * =========================================================
     */

    public JobStatus getStatus() {

        return status;
    }


    public void setStatus(
            JobStatus status
    ) {

        this.status = status;
    }


    /*
     * =========================================================
     * PROGRESS
     * =========================================================
     */

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


    /*
     * =========================================================
     * MESSAGE
     * =========================================================
     */

    public String getMessage() {

        return message;
    }


    public void setMessage(
            String message
    ) {

        this.message = message;
    }


    /*
     * =========================================================
     * ERROR
     * =========================================================
     */

    public String getError() {

        return error;
    }


    public void setError(
            String error
    ) {

        this.error = error;
    }


    /*
     * =========================================================
     * CREATED AT
     * =========================================================
     */

    public Instant getCreatedAt() {

        return createdAt;
    }


    public void setCreatedAt(
            Instant createdAt
    ) {

        this.createdAt = createdAt;
    }


    /*
     * =========================================================
     * STARTED AT
     * =========================================================
     */

    public Instant getStartedAt() {

        return startedAt;
    }


    public void setStartedAt(
            Instant startedAt
    ) {

        this.startedAt = startedAt;
    }


    /*
     * =========================================================
     * COMPLETED AT
     * =========================================================
     */

    public Instant getCompletedAt() {

        return completedAt;
    }


    public void setCompletedAt(
            Instant completedAt
    ) {

        this.completedAt = completedAt;
    }


    /*
     * =========================================================
     * PYTHON PROCESS
     * =========================================================
     */

    public Process getProcess() {

        return process;
    }


    public void setProcess(
            Process process
    ) {

        this.process = process;
    }
}