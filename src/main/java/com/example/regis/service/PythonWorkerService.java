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


    /*
     * ============================================================
     * EXECUTE PYTHON WORKER
     * ============================================================
     */
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


        Path accountFile =
                getUserAccountFile(
                        job.getOwnerUserId()
                );


        /*
         * Pastikan file account milik user memang ada.
         */
        if (!Files.exists(accountFile)) {

            failJob(
                    job,
                    "Account file not found",
                    "Account file does not exist: "
                            + accountFile
            );

            return;
        }


        /*
         * Jangan menjalankan Python jika file kosong.
         */
        try {

            if (
                    Files.size(accountFile) == 0
            ) {

                failJob(
                        job,
                        "Account file is empty",
                        "No account available for this user"
                );

                return;
            }

        } catch (Exception e) {

            failJob(
                    job,
                    "Failed to inspect account file",
                    e.getMessage()
            );

            return;
        }


        /*
         * ========================================================
         * COMMAND
         * ========================================================
         *
         * Contoh hasil:
         *
         * python
         * /home/vortexis/RegisV8_Fix/main.py
         * --links /.../link.txt
         * --accounts /.../accounts/17.txt
         * --mode account-per-link
         * --concurrency 10
         * --timeout 15
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


        /*
         * PENTING:
         *
         * Python sekarang membaca account file
         * MILIK USER yang membuat job.
         */
        command.add(
                accountFile.toString()
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


        Process process = null;


        try {

            /*
             * ====================================================
             * PROCESS BUILDER
             * ====================================================
             */
            ProcessBuilder processBuilder =
                    new ProcessBuilder(
                            command
                    );


            /*
             * Working directory:
             *
             * /home/vortexis/RegisV8_Fix
             */
            Path scriptPath =
                    Path.of(
                            properties.getScript()
                    );


            if (scriptPath.getParent() != null) {

                processBuilder.directory(
                        scriptPath
                                .getParent()
                                .toFile()
                );
            }


            /*
             * Gabungkan stderr + stdout.
             */
            processBuilder.redirectErrorStream(
                    true
            );


            /*
             * ====================================================
             * START
             * ====================================================
             */
            process =
                    processBuilder.start();


            job.setProcess(
                    process
            );


            job.setMessage(
                    "Python worker started"
            );


            /*
             * ====================================================
             * READ PYTHON OUTPUT
             * ====================================================
             *
             * Reader dijalankan secara terpisah supaya
             * waitFor(timeout) tetap bisa bekerja.
             */
            Process finalProcess =
                    process;


            Thread outputReader =
                    new Thread(
                            () ->
                                    readOutput(
                                            finalProcess,
                                            job
                                    )
                    );


            outputReader.setDaemon(
                    true
            );


            outputReader.start();


            /*
             * ====================================================
             * TIMEOUT
             * ====================================================
             *
             * timeout dari frontend dianggap MENIT.
             *
             * Contoh:
             *
             * timeout = 15
             *
             * berarti 15 menit.
             */
            long timeoutSeconds =
                    job.getTimeout() * 60L;


            boolean finished =
                    process.waitFor(
                            timeoutSeconds,
                            TimeUnit.SECONDS
                    );


            /*
             * ====================================================
             * TIMEOUT
             * ====================================================
             */
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


                /*
                 * Jangan mengubah CANCELLED menjadi FAILED.
                 */
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


            /*
             * Tunggu reader selesai membaca output.
             */
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


            /*
             * Jika user melakukan cancel
             * saat process berjalan.
             */
            if (
                    job.getStatus()
                            == JobStatus.CANCELLED
            ) {

                return;
            }


            int exitCode =
                    process.exitValue();


            /*
             * ====================================================
             * SUCCESS
             * ====================================================
             */
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


            }

            /*
             * ====================================================
             * FAILED
             * ====================================================
             */
            else {

                job.setStatus(
                        JobStatus.FAILED
                );


                job.setMessage(
                        "Python exited with code "
                                + exitCode
                );


                /*
                 * Output error sebenarnya sudah
                 * ditangani oleh readOutput().
                 *
                 * Kalau Python tidak memberikan output,
                 * tetap berikan pesan generic.
                 */
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

            /*
             * Jika process dibatalkan,
             * jangan overwrite CANCELLED menjadi FAILED.
             */
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


    /*
     * ============================================================
     * READ PYTHON OUTPUT
     * ============================================================
     */
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


                /*
                 * Jika Python gagal,
                 * simpan output sebagai error.
                 */
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

            /*
             * Jangan mengubah CANCELLED menjadi error.
             */
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


    /*
     * ============================================================
     * PROGRESS PARSER
     * ============================================================
     */
    private void parseProgress(
            Job job,
            String line
    ) {

        /*
         * Format Python:
         *
         * PROGRESS:50
         */
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

                // Ignore invalid progress.
            }
        }


        /*
         * Format Python:
         *
         * MESSAGE:Processing account 10
         */
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


    /*
     * ============================================================
     * USER ACCOUNT FILE
     * ============================================================
     */
    private Path getUserAccountFile(
            String userId
    ) {

        /*
         * Proteksi path traversal.
         */
        String safeUserId =
                userId.replaceAll(
                        "[^a-zA-Z0-9_-]",
                        "_"
                );


        return Path.of(
                properties.getAccountsDir(),
                safeUserId + ".txt"
        );
    }


    /*
     * ============================================================
     * FAIL JOB
     * ============================================================
     */
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