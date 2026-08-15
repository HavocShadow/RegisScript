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


        /*
         * Ini command sebenarnya:
         *
         * python main.py
         * --links ...
         * --accounts ...
         * --mode ...
         * --concurrency ...
         * --timeout ...
         *
         * Sesuai argparse pada main.py.
         */


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
                properties.getAccounts()
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


        try {

            ProcessBuilder processBuilder =
                    new ProcessBuilder(
                            command
                    );


            processBuilder.directory(
                    Path.of(
                            properties.getScript()
                    )
                    .getParent()
                    .toFile()
            );


            processBuilder.redirectErrorStream(
                    true
            );


            /*
             * START PROGRAM PYTHON
             */

            Process process =
                    processBuilder.start();


            job.setProcess(
                    process
            );


            StringBuilder output =
                    new StringBuilder();


            /*
             * Read stdout Python.
             */

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


                    output.append(
                            line
                    )
                    .append(
                            "\n"
                    );


                    /*
                     * Optional machine-readable
                     * progress.
                     */

                    parseProgress(
                            job,
                            line
                    );
                }
            }


            boolean finished =
                    process.waitFor(
                            job.getTimeout() * 60L,
                            TimeUnit.SECONDS
                    );


            if (!finished) {

                process.destroyForcibly();


                job.setStatus(
                        JobStatus.FAILED
                );


                job.setMessage(
                        "Python worker timeout"
                );


                job.setError(
                        "Worker exceeded timeout"
                );


                return;
            }


            if (
                    job.getStatus()
                            == JobStatus.CANCELLED
            ) {

                return;
            }


            int exitCode =
                    process.exitValue();


            if (exitCode == 0) {

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


                job.setError(
                        output.toString()
                );
            }


        } catch (
                Exception e
        ) {

            job.setStatus(
                    JobStatus.FAILED
            );


            job.setMessage(
                    "Failed to start Python"
            );


            job.setError(
                    e.getMessage()
            );


        } finally {

            job.setCompletedAt(
                    Instant.now()
            );


            job.setProcess(
                    null
            );
        }
    }


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
                                        9
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
                            8
                    ).trim()
            );
        }
    }
}