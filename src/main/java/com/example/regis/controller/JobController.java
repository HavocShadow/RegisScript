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


    public JobController(
            JobService service
    ) {

        this.service = service;
    }


    @PostMapping("/run")
    public ResponseEntity<JobResponse> run(
            @Valid
            @RequestBody
            CreateJobRequest request
    ) {

        return ResponseEntity
                .accepted()
                .body(
                        service.create(
                                request
                        )
                );
    }


    @GetMapping("/status")
    public ResponseEntity<JobResponse> status() {

        return ResponseEntity.ok(
                service.getCurrent()
        );
    }


    @PostMapping("/cancel")
    public ResponseEntity<Void> cancel(
            @RequestParam(
                    required = false
            )
            String reason
    ) {

        service.cancelCurrent(
                reason
        );

        return ResponseEntity
                .accepted()
                .build();
    }


    @GetMapping("/{jobId}")
    public ResponseEntity<JobResponse> get(
            @PathVariable String jobId
    ) {

        return ResponseEntity.ok(
                service.get(jobId)
        );
    }


    @PostMapping("/{jobId}/cancel")
    public ResponseEntity<Void> cancelById(
            @PathVariable String jobId
    ) {

        service.cancel(
                jobId
        );

        return ResponseEntity
                .accepted()
                .build();
    }
}