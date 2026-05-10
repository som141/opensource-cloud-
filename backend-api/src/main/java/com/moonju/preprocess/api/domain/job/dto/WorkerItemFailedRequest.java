package com.moonju.preprocess.api.domain.job.dto;

import jakarta.validation.constraints.NotBlank;

public record WorkerItemFailedRequest(
    @NotBlank String workerId,
    @NotBlank String errorCode,
    String errorMessage,
    boolean retryable
) {
}
