package com.moonju.preprocess.api.domain.job.dto;

import java.time.LocalDateTime;

public record WorkerArtifactRegisterResponse(
    Long jobId,
    Long itemId,
    boolean registered,
    LocalDateTime registeredAt
) {
}
