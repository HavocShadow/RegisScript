package com.example.regis.service;

import com.example.regis.config.WorkerProperties;
import com.example.regis.model.Job;
import com.example.regis.model.JobStatus;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.nio.file.Files;
import java.nio.file.Path;

import java.time.Instant;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.TimeUnit;

@Service
public class PythonWorkerService {

    private final WorkerProperties properties;

    public PythonWorkerService(
            WorkerProperties properties
    ) {
        this.properties = properties;
    }

    @Async("workerExecutor")
    public void execute(
            Job job
    ) {

        job.setStatus(
                JobStatus.RUNNING
        );

        job.setStartedAt(
                Instant.now()
        );

        job.setMessage(
                "Starting Python worker"
        );

        job.setProgress(
                0
        );

        // ========================================================
        // ACCOUNT FILE FROM JOB
        // ========================================================

        Path accountFile =
                Path.of(
                        job.getAccountFile()
                );

        if (
                !Files.exists(accountFile)
        ) {

            failJob(
                    job,
                    "Account file not found",
                    "Account file does not exist: "
                            + accountFile
            );

            return;
        }

        if (
                !Files.isRegularFile(accountFile)
        ) {

            failJob(
                    job,
                    "Invalid account file",
                    "Account path is not a regular file: "
                            + accountFile
            );

            return;
        }

        try {

            if (
                    Files.size(accountFile) == 0
            ) {

                failJob(
                        job,
                        "Account file is empty",
                        "No account available in: "
                                + accountFile
                );

                return;
            }

        } catch (
                Exception e
        ) {

            failJob(
                    job,
                    "Failed to inspect account file",
                    e.getMessage()
            );

            return;
        }

        // ========================================================
        // COMMAND
        // ========================================================

        List<String> command =
                new ArrayList<>();

        command.add(
                properties.getPython()
        );

        command.add(
                properties.getScript()
        );

        command.add(
                "--links"
        );

        command.add(
                properties.getLinks()
        );

        command.add(
                "--accounts"
        );

        command.add(
                accountFile
                        .toAbsolutePath()
                        .normalize()
                        .toString()
        );

        command.add(
                "--mode"
        );

        command.add(
                job.getMode()
        );

        command.add(
                "--concurrency"
        );

        command.add(
                String.valueOf(
                        job.getConcurrency()
                )
        );

        command.add(
                "--timeout"
        );

        command.add(
                String.valueOf(
                        job.getTimeout()
                )
        );

        Process process =
                null;

        try {

            ProcessBuilder processBuilder =
                    new ProcessBuilder(
                            command
                    );

            Path scriptPath =
                    Path.of(
                            properties.getScript()
                    );

            if (
                    scriptPath.getParent() != null
            ) {

                processBuilder.directory(
                        scriptPath
                                .getParent()
                                .toFile()
                );
            }

            processBuilder.redirectErrorStream(
                    true
            );

            process =
                    processBuilder.start();

            job.setProcess(
                    process
            );

            job.setMessage(
                    "Python worker started"
            );

            Process finalProcess =
                    process;

            Thread outputReader =
                    new Thread(
                            () ->
                                    readOutput(
                                            finalProcess,
                                            job
                                    ),
                            "python-output-"
                                    + job.getJobId()
                    );

            outputReader.setDaemon(
                    true
            );

            outputReader.start();

            // ====================================================
            // TIMEOUT
            // ====================================================

            long timeoutSeconds =
                    job.getTimeout() * 60L;

            boolean finished =
                    process.waitFor(
                            timeoutSeconds,
                            TimeUnit.SECONDS
                    );

            // ====================================================
            // TIMEOUT
            // ====================================================

            if (!finished) {

                process.destroy();

                if (
                        !process.waitFor(
                                3,
                                TimeUnit.SECONDS
                        )
                ) {

                    process.destroyForcibly();
                }

                if (
                        job.getStatus()
                                != JobStatus.CANCELLED
                ) {

                    failJob(
                            job,
                            "Python worker timeout",
                            "Worker exceeded timeout of "
                                    + job.getTimeout()
                                    + " minute(s)"
                    );
                }

                return;
            }

            // ====================================================
            // OUTPUT READER
            // ====================================================

            try {

                outputReader.join(
                        3000
                );

            } catch (
                    InterruptedException e
            ) {

                Thread.currentThread()
                        .interrupt();
            }

            if (
                    job.getStatus()
                            == JobStatus.CANCELLED
            ) {

                return;
            }

            int exitCode =
                    process.exitValue();

            // ====================================================
            // SUCCESS
            // ====================================================

            if (
                    exitCode == 0
            ) {

                job.setProgress(
                        100
                );

                job.setStatus(
                        JobStatus.COMPLETED
                );

                job.setMessage(
                        "Python worker completed"
                );

            } else {

                job.setStatus(
                        JobStatus.FAILED
                );

                job.setMessage(
                        "Python exited with code "
                                + exitCode
                );

                if (
                        job.getError() == null ||
                        job.getError().isBlank()
                ) {

                    job.setError(
                            "Python process exited with code "
                                    + exitCode
                    );
                }
            }

        } catch (
                Exception e
        ) {

            if (
                    job.getStatus()
                            != JobStatus.CANCELLED
            ) {

                failJob(
                        job,
                        "Failed to execute Python worker",
                        e.getMessage()
                );
            }

        } finally {

            job.setCompletedAt(
                    Instant.now()
            );

            job.setProcess(
                    null
            );
        }
    }

    // ============================================================
    // READ OUTPUT
    // ============================================================

    private void readOutput(
            Process process,
            Job job
    ) {

        try (
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        process.getInputStream()
                                )
                        )
        ) {

            String line;

            while (
                    (
                            line =
                                    reader.readLine()
                    ) != null
            ) {

                System.out.println(
                        "[PYTHON "
                                + job.getJobId()
                                + "] "
                                + line
                );

                parseProgress(
                        job,
                        line
                );

                if (
                        line.startsWith(
                                "ERROR:"
                        )
                ) {

                    job.setError(
                            line.substring(
                                    6
                            ).trim()
                    );
                }
            }

        } catch (
                Exception e
        ) {

            if (
                    job.getStatus()
                            != JobStatus.CANCELLED
            ) {

                job.setError(
                        e.getMessage()
                );
            }
        }
    }

    // ============================================================
    // PROGRESS
    // ============================================================

    private void parseProgress(
            Job job,
            String line
    ) {

        if (
                line.startsWith(
                        "PROGRESS:"
                )
        ) {

            try {

                int progress =
                        Integer.parseInt(
                                line.substring(
                                        "PROGRESS:"
                                                .length()
                                ).trim()
                        );

                job.setProgress(
                        progress
                );

            } catch (
                    NumberFormatException ignored
            ) {
            }
        }

        if (
                line.startsWith(
                        "MESSAGE:"
                )
        ) {

            job.setMessage(
                    line.substring(
                            "MESSAGE:"
                                    .length()
                    ).trim()
            );
        }
    }

    // ============================================================
    // FAIL JOB
    // ============================================================

    private void failJob(
            Job job,
            String message,
            String error
    ) {

        job.setStatus(
                JobStatus.FAILED
        );

        job.setMessage(
                message
        );

        job.setError(
                error == null
                        ? message
                        : error
        );

        job.setCompletedAt(
                Instant.now()
        );

        job.setProcess(
                null
        );
    }
}