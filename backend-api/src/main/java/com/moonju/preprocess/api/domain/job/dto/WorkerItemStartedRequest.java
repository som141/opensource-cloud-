package com.moonju.preprocess.api.domain.job.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record WorkerItemStartedRequest(
    @NotBlank String workerId,
    @Min(1) Integer attempt
) {
}
