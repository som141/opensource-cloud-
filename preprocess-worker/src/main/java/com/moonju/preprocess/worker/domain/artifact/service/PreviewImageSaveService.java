package com.moonju.preprocess.worker.domain.artifact.service;

import com.moonju.preprocess.worker.domain.artifact.dto.ArtifactUploadRequest;
import com.moonju.preprocess.worker.domain.artifact.dto.ArtifactUploadResult;
import com.moonju.preprocess.worker.domain.artifact.model.ArtifactPath;
import com.moonju.preprocess.worker.domain.artifact.model.ArtifactType;
import org.springframework.stereotype.Service;

@Service
public class PreviewImageSaveService {

    private final ArtifactSaveService artifactSaveService;

    public PreviewImageSaveService(ArtifactSaveService artifactSaveService) {
        this.artifactSaveService = artifactSaveService;
    }

    public ArtifactUploadResult prepare(Long projectId, Long jobId, Long itemId) {
        ArtifactPath path = ArtifactPath.forType(ArtifactType.PREVIEW_IMAGE, projectId, jobId, itemId);
        return artifactSaveService.prepareUpload(ArtifactUploadRequest.skeleton(ArtifactType.PREVIEW_IMAGE, path));
    }
}
