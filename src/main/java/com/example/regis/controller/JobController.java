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


    @PostMapping
    public ResponseEntity<JobResponse> create(
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


    @GetMapping("/{jobId}")
    public ResponseEntity<JobResponse> get(
            @PathVariable String jobId
    ) {

        return ResponseEntity.ok(
                service.get(
                        jobId
                )
        );
    }


    @PostMapping("/{jobId}/cancel")
    public ResponseEntity<Void> cancel(
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