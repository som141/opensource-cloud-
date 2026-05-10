package com.moonju.preprocess.api.domain.job.dto;

import com.moonju.preprocess.api.domain.job.entity.JobItemStatus;
import java.time.LocalDateTime;

public record WorkerItemReportResponse(
    Long jobId,
    Long itemId,
    JobItemStatus status,
    String workerId,
    LocalDateTime reportedAt
) {
}
