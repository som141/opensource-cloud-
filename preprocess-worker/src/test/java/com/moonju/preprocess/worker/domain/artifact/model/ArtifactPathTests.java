package com.moonju.preprocess.worker.domain.artifact.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ArtifactPathTests {

    @Test
    void buildsProcessedPreviewAndReportPaths() {
        assertThat(ArtifactPath.forType(ArtifactType.PROCESSED_IMAGE, 1L, 2L, 3L).value())
            .isEqualTo("processed/1/2/3/processed.png");
        assertThat(ArtifactPath.forType(ArtifactType.PREVIEW_IMAGE, 1L, 2L, 3L).value())
            .isEqualTo("processed/1/2/3/preview.png");
        assertThat(ArtifactPath.forType(ArtifactType.PROCESSING_REPORT, 1L, 2L, 3L).value())
            .isEqualTo("processed/1/2/3/processing-report.json");
    }

    @Test
    void buildsDebugArtifactPath() {
        ArtifactPath path = ArtifactPath.debug(1L, 2L, 3L, "02_deskew.png");

        assertThat(path.value()).isEqualTo("processed/1/2/3/debug/02_deskew.png");
    }

    @Test
    void rejectsDebugPathWithoutStepFileName() {
        assertThatThrownBy(() -> ArtifactPath.debug(1L, 2L, 3L, " "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Debug step file name is required.");
    }
}
