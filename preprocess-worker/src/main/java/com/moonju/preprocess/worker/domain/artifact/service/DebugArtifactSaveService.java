package com.moonju.preprocess.worker.domain.artifact.service;

import com.moonju.preprocess.worker.domain.artifact.dto.ArtifactUploadRequest;
import com.moonju.preprocess.worker.domain.artifact.dto.ArtifactUploadResult;
import com.moonju.preprocess.worker.domain.artifact.exception.ArtifactUploadFailedException;
import com.moonju.preprocess.worker.domain.artifact.model.ArtifactPath;
import com.moonju.preprocess.worker.domain.artifact.model.ArtifactType;
import com.moonju.preprocess.worker.domain.preprocess.model.DebugArtifactSnapshot;
import com.moonju.preprocess.worker.domain.preprocess.service.ImageEncodePort;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DebugArtifactSaveService {

    private final ArtifactSaveService artifactSaveService;
    private final ImageEncodePort imageEncodePort;

    public DebugArtifactSaveService(ArtifactSaveService artifactSaveService, ImageEncodePort imageEncodePort) {
        this.artifactSaveService = artifactSaveService;
        this.imageEncodePort = imageEncodePort;
    }

    public ArtifactUploadResult prepare(Long projectId, Long jobId, Long itemId, String stepFileName) {
        ArtifactPath path = ArtifactPath.debug(projectId, jobId, itemId, stepFileName);
        return artifactSaveService.prepareUpload(ArtifactUploadRequest.skeleton(ArtifactType.DEBUG_IMAGE, path));
    }

    public List<ArtifactUploadResult> saveAll(List<DebugArtifactSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return List.of();
        }
        return snapshots.stream()
            .filter(DebugArtifactSnapshot::loaded)
            .map(this::save)
            .toList();
    }

    public ArtifactUploadResult save(DebugArtifactSnapshot snapshot) {
        ArtifactPath path = new ArtifactPath(snapshot.descriptor().objectKey());
        try {
            byte[] content = imageEncodePort.encodePng(path.value(), snapshot.image());
            return artifactSaveService.upload(ArtifactUploadRequest.upload(ArtifactType.DEBUG_IMAGE, path, content));
        } catch (ArtifactUploadFailedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ArtifactUploadFailedException("Debug artifact save failed: " + path.value(), exception);
        }
    }
}
