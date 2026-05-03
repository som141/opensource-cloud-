package com.moonju.preprocess.api.domain.project.dto;

public record ProjectSummaryResponse(
    Long projectId,
    long imageCount,
    long jobCount,
    long succeededJobCount,
    long failedJobCount
) {

    public static ProjectSummaryResponse empty(Long projectId) {
        return new ProjectSummaryResponse(projectId, 0L, 0L, 0L, 0L);
    }
}
