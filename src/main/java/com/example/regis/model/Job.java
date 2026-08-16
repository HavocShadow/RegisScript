package com.example.regis.model;

import java.time.Instant;

public class Job {

    private final String jobId;

    private final String mode;

    private final int concurrency;

    private final int timeout;

    private final Instant createdAt;


    private volatile JobStatus status =
            JobStatus.QUEUED;

    private volatile int progress = 0;

    private volatile String message =
            "Queued";

    private volatile String error;

    private volatile Instant startedAt;

    private volatile Instant completedAt;

    private volatile Process process;


    public Job(
            String jobId,
            String mode,
            int concurrency,
            int timeout
    ) {

        this.jobId = jobId;

        this.mode = mode;

        this.concurrency = concurrency;

        this.timeout = timeout;

        this.createdAt =
                Instant.now();
    }


    public String getJobId() {
        return jobId;
    }


    public String getMode() {
        return mode;
    }


    public int getConcurrency() {
        return concurrency;
    }


    public int getTimeout() {
        return timeout;
    }


    public Instant getCreatedAt() {
        return createdAt;
    }


    public JobStatus getStatus() {
        return status;
    }


    public void setStatus(
            JobStatus status
    ) {

        this.status = status;
    }


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


    public String getMessage() {
        return message;
    }


    public void setMessage(
            String message
    ) {

        this.message = message;
    }


    public String getError() {
        return error;
    }


    public void setError(
            String error
    ) {

        this.error = error;
    }


    public Instant getStartedAt() {
        return startedAt;
    }


    public void setStartedAt(
            Instant startedAt
    ) {

        this.startedAt =
                startedAt;
    }


    public Instant getCompletedAt() {
        return completedAt;
    }


    public void setCompletedAt(
            Instant completedAt
    ) {

        this.completedAt =
                completedAt;
    }


    public Process getProcess() {
        return process;
    }


    public void setProcess(
            Process process
    ) {

        this.process = process;
    }
}