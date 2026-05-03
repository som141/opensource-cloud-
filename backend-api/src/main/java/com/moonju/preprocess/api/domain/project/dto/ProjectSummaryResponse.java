package com.moonju.preprocess.api.domain.project.dto;

public record ProjectSummaryResponse(
    Long projectId,
    String name,
    long memberCount,
    long imageCount,
    long jobCount
) {
}
