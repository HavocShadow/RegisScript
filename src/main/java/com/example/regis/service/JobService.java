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


    public JobResponse create(
            CreateJobRequest request
    ) {

        String mode =
                request.mode() == null
                        ? "account-per-link"
                        : request.mode();


        if (
                !Set.of(
                        "account-per-link",
                        "link-per-account",
                        "one-to-one",
                        "repeat-accounts"
                ).contains(mode)
        ) {

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


        if (
                concurrency >
                        properties.getMaxConcurrency()
        ) {

            throw new IllegalArgumentException(
                    "Concurrency exceeds limit"
            );
        }


        if (
                timeout >
                        properties.getMaxTimeoutSeconds()
        ) {

            throw new IllegalArgumentException(
                    "Timeout exceeds limit"
            );
        }


        String jobId =
                UUID.randomUUID()
                        .toString();


        Job job =
                new Job(
                        jobId,
                        mode,
                        concurrency,
                        timeout
                );


        jobs.put(
                jobId,
                job
        );


        worker.execute(
                job
        );


        return get(
                jobId
        );
    }


    public JobResponse get(
            String jobId
    ) {

        Job job =
                require(jobId);


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


    public void cancel(
            String jobId
    ) {

        Job job =
                require(jobId);


        Process process =
                job.getProcess();


        if (
                process != null &&
                process.isAlive()
        ) {

            job.setStatus(
                    JobStatus.CANCELLED
            );


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


            job.setCompletedAt(
                    Instant.now()
            );
        }
    }


    private Job require(
            String jobId
    ) {

        Job job =
                jobs.get(jobId);


        if (job == null) {

            throw new IllegalArgumentException(
                    "Job not found"
            );
        }


        return job;
    }
}