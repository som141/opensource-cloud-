package com.moonju.preprocess.worker.domain.artifact.service;

import com.moonju.preprocess.worker.domain.artifact.dto.ArtifactUploadRequest;
import com.moonju.preprocess.worker.domain.artifact.dto.ArtifactUploadResult;
import com.moonju.preprocess.worker.domain.artifact.model.ArtifactPath;
import com.moonju.preprocess.worker.domain.artifact.model.ArtifactType;
import org.springframework.stereotype.Service;

@Service
public class ProcessedImageSaveService {

    private final ArtifactSaveService artifactSaveService;

    public ProcessedImageSaveService(ArtifactSaveService artifactSaveService) {
        this.artifactSaveService = artifactSaveService;
    }

    public ArtifactUploadResult prepare(Long projectId, Long jobId, Long itemId) {
        ArtifactPath path = ArtifactPath.forType(ArtifactType.PROCESSED_IMAGE, projectId, jobId, itemId);
        return artifactSaveService.prepareUpload(ArtifactUploadRequest.skeleton(ArtifactType.PROCESSED_IMAGE, path));
    }
}
