package com.example.regis.dto;

import com.example.regis.model.JobStatus;

import java.time.Instant;

public record JobResponse(

        String jobId,

        String mode,

        int concurrency,

        int timeout,

        JobStatus status,

        int progress,

        String message,

        Instant createdAt,

        Instant startedAt,

        Instant completedAt,

        String error

) {
}
