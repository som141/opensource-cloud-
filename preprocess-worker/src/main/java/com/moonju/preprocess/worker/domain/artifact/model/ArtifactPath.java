package com.moonju.preprocess.worker.domain.artifact.model;

public record ArtifactPath(String value) {

    public static ArtifactPath forType(ArtifactType type, Long projectId, Long jobId, Long itemId) {
        if (type.debug()) {
            throw new IllegalArgumentException("Debug artifact path requires a step file name.");
        }
        return new ArtifactPath(basePath(projectId, jobId, itemId) + "/" + type.defaultFileName());
    }

    public static ArtifactPath debug(Long projectId, Long jobId, Long itemId, String stepFileName) {
        if (stepFileName == null || stepFileName.isBlank()) {
            throw new IllegalArgumentException("Debug step file name is required.");
        }
        return new ArtifactPath(basePath(projectId, jobId, itemId) + "/debug/" + stepFileName);
    }

    private static String basePath(Long projectId, Long jobId, Long itemId) {
        if (projectId == null || jobId == null || itemId == null) {
            throw new IllegalArgumentException("Artifact path identity values are required.");
        }
        return "processed/" + projectId + "/" + jobId + "/" + itemId;
    }
}
