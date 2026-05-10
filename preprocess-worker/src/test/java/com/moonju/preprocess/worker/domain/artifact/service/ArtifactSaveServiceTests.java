package com.moonju.preprocess.worker.domain.artifact.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.moonju.preprocess.worker.domain.artifact.dto.ArtifactUploadResult;
import com.moonju.preprocess.worker.domain.artifact.model.ArtifactType;
import org.junit.jupiter.api.Test;

class ArtifactSaveServiceTests {

    private final ArtifactSaveService artifactSaveService = new ArtifactSaveService();

    @Test
    void preparesProcessedPreviewAndDebugArtifactsWithoutUploading() {
        ProcessedImageSaveService processedImageSaveService = new ProcessedImageSaveService(artifactSaveService);
        PreviewImageSaveService previewImageSaveService = new PreviewImageSaveService(artifactSaveService);
        DebugArtifactSaveService debugArtifactSaveService = new DebugArtifactSaveService(artifactSaveService);

        ArtifactUploadResult processed = processedImageSaveService.prepare(1L, 2L, 3L);
        ArtifactUploadResult preview = previewImageSaveService.prepare(1L, 2L, 3L);
        ArtifactUploadResult debug = debugArtifactSaveService.prepare(1L, 2L, 3L, "00_decoded.png");

        assertThat(processed.artifactType()).isEqualTo(ArtifactType.PROCESSED_IMAGE);
        assertThat(processed.uploaded()).isFalse();
        assertThat(processed.artifactPath().value()).isEqualTo("processed/1/2/3/processed.png");
        assertThat(preview.artifactPath().value()).isEqualTo("processed/1/2/3/preview.png");
        assertThat(debug.artifactPath().value()).isEqualTo("processed/1/2/3/debug/00_decoded.png");
    }
}
