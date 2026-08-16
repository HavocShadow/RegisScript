package com.example.regis.service;

import com.example.regis.config.WorkerProperties;
import com.example.regis.model.Job;
import com.example.regis.model.JobStatus;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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

        List<String> command =
                new ArrayList<>();

        command.add(
                properties.getPython()
        );

        command.add(
                properties.getScript()
        );

        command.add("--links");
        command.add(properties.getLinks());

        command.add("--accounts");
        command.add(properties.getAccounts());

        command.add("--mode");
        command.add(job.getMode());

        command.add("--concurrency");
        command.add(
                String.valueOf(
                        job.getConcurrency()
                )
        );

        command.add("--timeout");
        command.add(
                String.valueOf(
                        job.getTimeout()
                )
        );

        Process process = null;

        try {

            ProcessBuilder processBuilder =
                    new ProcessBuilder(command);

            String script =
                    properties.getScript();

            Path scriptPath =
                    Path.of(script);

            if (scriptPath.getParent() != null) {

                processBuilder.directory(
                        scriptPath
                                .getParent()
                                .toFile()
                );
            }

            processBuilder.redirectErrorStream(true);

            process =
                    processBuilder.start();

            job.setProcess(process);

            StringBuilder output =
                    new StringBuilder();

            /*
             * Read Python output in a separate thread.
             * This prevents stdout from blocking the process.
             */
            Process finalProcess = process;

            Thread outputReader =
                    new Thread(() -> {

                        try (
                                BufferedReader reader =
                                        new BufferedReader(
                                                new InputStreamReader(
                                                        finalProcess
                                                                .getInputStream()
                                                )
                                        )
                        ) {

                            String line;

                            while (
                                    (line =
                                            reader.readLine())
                                            != null
                            ) {

                                System.out.println(
                                        "[PYTHON "
                                                + job.getJobId()
                                                + "] "
                                                + line
                                );

                                synchronized (output) {

                                    output
                                            .append(line)
                                            .append("\n");
                                }

                                parseProgress(
                                        job,
                                        line
                                );
                            }

                        } catch (Exception e) {

                            System.err.println(
                                    "[PYTHON "
                                            + job.getJobId()
                                            + "] "
                                            + "Output reader error: "
                                            + e.getMessage()
                            );
                        }

                    });

            outputReader.setDaemon(true);

            outputReader.start();

            /*
             * timeout is interpreted as MINUTES.
             */
            long timeoutSeconds =
                    job.getTimeout() * 60L;

            boolean finished =
                    process.waitFor(
                            timeoutSeconds,
                            TimeUnit.SECONDS
                    );

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

                job.setStatus(
                        JobStatus.FAILED
                );

                job.setMessage(
                        "Python worker timeout"
                );

                job.setError(
                        "Worker exceeded "
                                + job.getTimeout()
                                + " minute(s)"
                );

                return;
            }

            /*
             * Wait briefly for stdout reader to finish.
             */
            outputReader.join(2000);

            if (
                    job.getStatus()
                            == JobStatus.CANCELLED
            ) {

                return;
            }

            int exitCode =
                    process.exitValue();

            if (exitCode == 0) {

                job.setProgress(100);

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

                synchronized (output) {

                    job.setError(
                            output.toString()
                    );
                }
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            if (
                    process != null &&
                    process.isAlive()
            ) {

                process.destroyForcibly();
            }

            job.setStatus(
                    JobStatus.FAILED
            );

            job.setMessage(
                    "Python worker interrupted"
            );

            job.setError(
                    e.getMessage()
            );

        } catch (Exception e) {

            job.setStatus(
                    JobStatus.FAILED
            );

            job.setMessage(
                    "Failed to execute Python worker"
            );

            job.setError(
                    e.getClass().getSimpleName()
                            + ": "
                            + e.getMessage()
            );

        } finally {

            job.setCompletedAt(
                    Instant.now()
            );

            job.setProcess(null);
        }
    }

    private void parseProgress(
            Job job,
            String line
    ) {

        if (
                line.startsWith("PROGRESS:")
        ) {

            try {

                int progress =
                        Integer.parseInt(
                                line.substring(
                                        "PROGRESS:"
                                                .length()
                                ).trim()
                        );

                progress =
                        Math.max(
                                0,
                                Math.min(
                                        100,
                                        progress
                                )
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
                line.startsWith("MESSAGE:")
        ) {

            job.setMessage(
                    line.substring(
                            "MESSAGE:"
                                    .length()
                    ).trim()
            );
        }
    }
}
