package com.moonju.preprocess.api.domain.job.dto;

import jakarta.validation.constraints.NotBlank;

public record WorkerItemHeartbeatRequest(
    @NotBlank String workerId
) {
}
