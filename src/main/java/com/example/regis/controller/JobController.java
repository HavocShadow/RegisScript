package com.example.regis.controller;

import com.example.regis.dto.CreateJobRequest;
import com.example.regis.dto.JobResponse;
import com.example.regis.service.JobService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/jobs")
@CrossOrigin(origins = "${app.cors-origin}")
public class JobController {

    private final JobService service;

    public JobController(JobService service) {
        this.service = service;
    }

    /**
     * Create / run job
     *
     * POST /api/v1/jobs/run
     */
    @PostMapping("/run")
    public ResponseEntity<JobResponse> run(
            @Valid @RequestBody CreateJobRequest request
    ) {

        return ResponseEntity
                .accepted()
                .body(service.create(request));
    }

    /**
     * Get current/latest job
     *
     * GET /api/v1/jobs/status
     */
    @GetMapping("/status")
    public ResponseEntity<JobResponse> status() {

        return ResponseEntity.ok(
                service.getCurrent()
        );
    }

    /**
     * Cancel current/latest job
     *
     * POST /api/v1/jobs/cancel
     */
    @PostMapping("/cancel")
    public ResponseEntity<Void> cancel(
            @RequestParam(required = false) String reason
    ) {

        service.cancelCurrent(reason);

        return ResponseEntity.accepted().build();
    }

    /**
     * Get specific job
     *
     * GET /api/v1/jobs/{jobId}
     */
    @GetMapping("/{jobId}")
    public ResponseEntity<JobResponse> get(
            @PathVariable String jobId
    ) {

        return ResponseEntity.ok(
                service.get(jobId)
        );
    }

    /**
     * Cancel specific job
     *
     * POST /api/v1/jobs/{jobId}/cancel
     */
    @PostMapping("/{jobId}/cancel")
    public ResponseEntity<Void> cancelById(
            @PathVariable String jobId
    ) {

        service.cancel(jobId);

        return ResponseEntity.accepted().build();
    }
}
