package com.moonju.preprocess.worker.domain.artifact.service;

import com.moonju.preprocess.worker.domain.artifact.dto.ArtifactUploadRequest;
import com.moonju.preprocess.worker.domain.artifact.dto.ArtifactUploadResult;
import org.springframework.stereotype.Service;

@Service
public class ArtifactSaveService {

    public ArtifactUploadResult prepareUpload(ArtifactUploadRequest request) {
        return ArtifactUploadResult.prepared(request);
    }
}
