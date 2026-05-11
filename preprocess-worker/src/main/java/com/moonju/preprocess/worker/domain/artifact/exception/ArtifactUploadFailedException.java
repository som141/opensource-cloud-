package com.moonju.preprocess.worker.domain.artifact.exception;

public class ArtifactUploadFailedException extends RuntimeException {

    public ArtifactUploadFailedException(String message) {
        super(message);
    }

    public ArtifactUploadFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
