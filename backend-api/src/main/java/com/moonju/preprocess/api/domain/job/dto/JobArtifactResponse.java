package com.moonju.preprocess.api.domain.job.dto;

public record JobArtifactResponse(
    Long jobId,
    String message
) {

    public static JobArtifactResponse skeleton(Long jobId) {
        return new JobArtifactResponse(jobId, "Job artifact listing is reserved for worker artifact integration.");
    }
}
