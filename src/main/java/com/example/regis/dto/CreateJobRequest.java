package com.example.regis.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CreateJobRequest(

        String mode,

        @Min(1)
        @Max(50)
        Integer concurrency,

        @Min(1)
        @Max(300)
        Integer timeout

) {
}
