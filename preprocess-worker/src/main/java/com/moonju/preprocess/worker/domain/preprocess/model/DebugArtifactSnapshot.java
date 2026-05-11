package com.moonju.preprocess.worker.domain.preprocess.model;

import com.moonju.preprocess.worker.domain.preprocess.step.PreprocessStepName;
import org.opencv.core.Mat;

public final class DebugArtifactSnapshot implements AutoCloseable {

    private final DebugArtifactDescriptor descriptor;
    private final Mat image;
    private boolean released;

    private DebugArtifactSnapshot(DebugArtifactDescriptor descriptor, Mat image) {
        this.descriptor = descriptor;
        this.image = image;
    }

    public static DebugArtifactSnapshot image(
        PreprocessStepName stepName,
        Long projectId,
        Long jobId,
        Long itemId,
        String fileName,
        Mat source
    ) {
        if (source == null || source.empty()) {
            throw new IllegalArgumentException("Debug artifact source image is required.");
        }
        DebugArtifactDescriptor descriptor = DebugArtifactDescriptor.image(stepName, projectId, jobId, itemId, fileName);
        return new DebugArtifactSnapshot(descriptor, source.clone());
    }

    public DebugArtifactDescriptor descriptor() {
        return descriptor;
    }

    public Mat image() {
        return image;
    }

    public boolean loaded() {
        return !released && image != null && !image.empty();
    }

    public void release() {
        if (!released && image != null) {
            image.release();
        }
        released = true;
    }

    @Override
    public void close() {
        release();
    }
}
