package com.moonju.preprocess.worker.domain.artifact.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.worker.domain.artifact.dto.ArtifactUploadResult;
import com.moonju.preprocess.worker.domain.artifact.model.ArtifactType;
import com.moonju.preprocess.worker.domain.preprocess.model.ImageMatHolder;
import com.moonju.preprocess.worker.domain.preprocess.service.ImageEncodePort;
import com.moonju.preprocess.worker.domain.report.dto.ProcessingReport;
import com.moonju.preprocess.worker.domain.report.model.ProcessingFallbackSummary;
import com.moonju.preprocess.worker.domain.report.model.ProcessingMemoryUsage;
import com.moonju.preprocess.worker.domain.report.model.ProcessingTiming;
import com.moonju.preprocess.worker.domain.report.service.ProcessingReportWriter;
import com.moonju.preprocess.worker.infra.opencv.OpenCvLoader;
import com.moonju.preprocess.worker.infra.storage.ObjectStoragePort;
import java.util.Arrays;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

class ArtifactSaveServiceTests {

    private final ObjectStoragePort objectStoragePort = mock(ObjectStoragePort.class);
    private final ImageEncodePort imageEncodePort = mock(ImageEncodePort.class);
    private final ArtifactSaveService artifactSaveService = new ArtifactSaveService(objectStoragePort);

    @BeforeAll
    static void loadOpenCv() {
        new OpenCvLoader().loadIfPresent();
    }

    @Test
    void preparesProcessedPreviewAndDebugArtifactsWithoutUploading() {
        ProcessedImageSaveService processedImageSaveService = new ProcessedImageSaveService(
            artifactSaveService,
            imageEncodePort
        );
        PreviewImageSaveService previewImageSaveService = new PreviewImageSaveService(
            artifactSaveService,
            imageEncodePort
        );
        DebugArtifactSaveService debugArtifactSaveService = new DebugArtifactSaveService(artifactSaveService);

        ArtifactUploadResult processed = processedImageSaveService.prepare(1L, 2L, 3L);
        ArtifactUploadResult preview = previewImageSaveService.prepare(1L, 2L, 3L);
        ArtifactUploadResult debug = debugArtifactSaveService.prepare(1L, 2L, 3L, "00_decoded.png");

        assertThat(processed.artifactType()).isEqualTo(ArtifactType.PROCESSED_IMAGE);
        assertThat(processed.uploaded()).isFalse();
        assertThat(processed.sizeBytes()).isZero();
        assertThat(processed.artifactPath().value()).isEqualTo("processed/1/2/3/processed.png");
        assertThat(preview.artifactPath().value()).isEqualTo("processed/1/2/3/preview.png");
        assertThat(debug.artifactPath().value()).isEqualTo("processed/1/2/3/debug/00_decoded.png");
    }

    @Test
    void uploadsProcessedAndPreviewArtifacts() {
        ProcessedImageSaveService processedImageSaveService = new ProcessedImageSaveService(
            artifactSaveService,
            imageEncodePort
        );
        PreviewImageSaveService previewImageSaveService = new PreviewImageSaveService(
            artifactSaveService,
            imageEncodePort
        );
        ImageMatHolder holder = ImageMatHolder.decoded(
            "originals/scan.png",
            new Mat(2, 3, CvType.CV_8UC1, new Scalar(255))
        );
        when(imageEncodePort.encodePng(eq("processed/1/2/3/processed.png"), eq(holder.mat())))
            .thenReturn(new byte[] {1, 2, 3});
        when(imageEncodePort.encodePng(eq("processed/1/2/3/preview.png"), org.mockito.ArgumentMatchers.any()))
            .thenReturn(new byte[] {4, 5});

        ArtifactUploadResult processed = processedImageSaveService.save(1L, 2L, 3L, holder);
        ArtifactUploadResult preview = previewImageSaveService.save(1L, 2L, 3L, holder);

        assertThat(processed.uploaded()).isTrue();
        assertThat(processed.sizeBytes()).isEqualTo(3);
        assertThat(preview.uploaded()).isTrue();
        assertThat(preview.sizeBytes()).isEqualTo(2);
        verify(objectStoragePort).uploadBytes(
            eq("processed/1/2/3/processed.png"),
            org.mockito.ArgumentMatchers.argThat(bytes -> Arrays.equals(bytes, new byte[] {1, 2, 3})),
            eq("image/png")
        );
        verify(objectStoragePort).uploadBytes(
            eq("processed/1/2/3/preview.png"),
            org.mockito.ArgumentMatchers.argThat(bytes -> Arrays.equals(bytes, new byte[] {4, 5})),
            eq("image/png")
        );
        holder.release();
    }

    @Test
    void uploadsProcessingReportArtifact() {
        ProcessingReportSaveService reportSaveService = new ProcessingReportSaveService(
            artifactSaveService,
            new ProcessingReportWriter()
        );
        ProcessingReport report = new ProcessingReport(
            2L,
            3L,
            "A4_SCAN_300DPI",
            List.of(),
            ProcessingTiming.wallOnly(Duration.ofMillis(10)),
            ProcessingMemoryUsage.notSampled(),
            ProcessingFallbackSummary.empty(),
            List.of(),
            true,
            null
        );

        ArtifactUploadResult result = reportSaveService.save(1L, 2L, 3L, report);

        assertThat(result.artifactType()).isEqualTo(ArtifactType.PROCESSING_REPORT);
        assertThat(result.artifactPath().value()).isEqualTo("processed/1/2/3/processing-report.json");
        assertThat(result.uploaded()).isTrue();
        assertThat(result.sizeBytes()).isPositive();
        verify(objectStoragePort).uploadBytes(
            eq("processed/1/2/3/processing-report.json"),
            org.mockito.ArgumentMatchers.any(),
            eq("application/json")
        );
    }
}
