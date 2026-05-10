package com.moonju.preprocess.worker.domain.preprocess.model;

import com.moonju.preprocess.worker.domain.artifact.model.ArtifactPath;
import com.moonju.preprocess.worker.domain.artifact.model.ArtifactType;
import com.moonju.preprocess.worker.domain.preprocess.step.PreprocessStepName;

public record DebugArtifactDescriptor(
    PreprocessStepName stepName,
    String fileName,
    String objectKey,
    String contentType
) {

    public static DebugArtifactDescriptor image(
        PreprocessStepName stepName,
        Long projectId,
        Long jobId,
        Long itemId,
        String fileName
    ) {
        if (stepName == null) {
            throw new IllegalArgumentException("Debug artifact step name is required.");
        }
        ArtifactPath path = ArtifactPath.debug(projectId, jobId, itemId, fileName);
        return new DebugArtifactDescriptor(stepName, fileName, path.value(), ArtifactType.DEBUG_IMAGE.contentType());
    }
}
