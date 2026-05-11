package com.moonju.preprocess.worker.domain.artifact.service;

import com.moonju.preprocess.worker.domain.artifact.dto.ArtifactUploadRequest;
import com.moonju.preprocess.worker.domain.artifact.dto.ArtifactUploadResult;
import com.moonju.preprocess.worker.domain.artifact.exception.ArtifactUploadFailedException;
import com.moonju.preprocess.worker.domain.artifact.model.ArtifactPath;
import com.moonju.preprocess.worker.domain.artifact.model.ArtifactType;
import com.moonju.preprocess.worker.domain.preprocess.model.ImageMatHolder;
import com.moonju.preprocess.worker.domain.preprocess.service.ImageEncodePort;
import org.springframework.stereotype.Service;

@Service
public class ProcessedImageSaveService {

    private final ArtifactSaveService artifactSaveService;
    private final ImageEncodePort imageEncodePort;

    public ProcessedImageSaveService(ArtifactSaveService artifactSaveService, ImageEncodePort imageEncodePort) {
        this.artifactSaveService = artifactSaveService;
        this.imageEncodePort = imageEncodePort;
    }

    public ArtifactUploadResult prepare(Long projectId, Long jobId, Long itemId) {
        ArtifactPath path = ArtifactPath.forType(ArtifactType.PROCESSED_IMAGE, projectId, jobId, itemId);
        return artifactSaveService.prepareUpload(ArtifactUploadRequest.skeleton(ArtifactType.PROCESSED_IMAGE, path));
    }

    public ArtifactUploadResult save(Long projectId, Long jobId, Long itemId, ImageMatHolder processedImage) {
        ArtifactPath path = ArtifactPath.forType(ArtifactType.PROCESSED_IMAGE, projectId, jobId, itemId);
        try {
            byte[] content = imageEncodePort.encodePng(path.value(), processedImage.mat());
            ArtifactUploadRequest request = ArtifactUploadRequest.upload(
                ArtifactType.PROCESSED_IMAGE,
                path,
                content
            );
            return artifactSaveService.upload(request);
        } catch (ArtifactUploadFailedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ArtifactUploadFailedException("Processed image artifact save failed: " + path.value(), exception);
        }
    }
}
