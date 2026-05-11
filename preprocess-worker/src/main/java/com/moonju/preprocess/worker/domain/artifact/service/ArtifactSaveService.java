package com.moonju.preprocess.worker.domain.artifact.service;

import com.moonju.preprocess.worker.domain.artifact.dto.ArtifactUploadRequest;
import com.moonju.preprocess.worker.domain.artifact.dto.ArtifactUploadResult;
import com.moonju.preprocess.worker.domain.artifact.exception.ArtifactUploadFailedException;
import com.moonju.preprocess.worker.infra.storage.ObjectStoragePort;
import org.springframework.stereotype.Service;

@Service
public class ArtifactSaveService {

    private final ObjectStoragePort objectStoragePort;

    public ArtifactSaveService(ObjectStoragePort objectStoragePort) {
        this.objectStoragePort = objectStoragePort;
    }

    public ArtifactUploadResult prepareUpload(ArtifactUploadRequest request) {
        return ArtifactUploadResult.prepared(request);
    }

    public ArtifactUploadResult upload(ArtifactUploadRequest request) {
        if (request.content().length == 0) {
            throw new ArtifactUploadFailedException("Artifact content is empty: " + request.artifactPath().value());
        }
        try {
            objectStoragePort.uploadBytes(request.artifactPath().value(), request.content(), request.contentType());
            return ArtifactUploadResult.uploaded(request);
        } catch (RuntimeException exception) {
            throw new ArtifactUploadFailedException(
                "Artifact upload failed: " + request.artifactPath().value(),
                exception
            );
        }
    }
}
