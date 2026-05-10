package com.moonju.preprocess.worker.domain.artifact.service;

import com.moonju.preprocess.worker.domain.artifact.dto.ArtifactUploadRequest;
import com.moonju.preprocess.worker.domain.artifact.dto.ArtifactUploadResult;
import com.moonju.preprocess.worker.domain.artifact.model.ArtifactPath;
import com.moonju.preprocess.worker.domain.artifact.model.ArtifactType;
import org.springframework.stereotype.Service;

@Service
public class DebugArtifactSaveService {

    private final ArtifactSaveService artifactSaveService;

    public DebugArtifactSaveService(ArtifactSaveService artifactSaveService) {
        this.artifactSaveService = artifactSaveService;
    }

    public ArtifactUploadResult prepare(Long projectId, Long jobId, Long itemId, String stepFileName) {
        ArtifactPath path = ArtifactPath.debug(projectId, jobId, itemId, stepFileName);
        return artifactSaveService.prepareUpload(ArtifactUploadRequest.skeleton(ArtifactType.DEBUG_IMAGE, path));
    }
}
