package com.moonju.preprocess.worker.domain.artifact.service;

import com.moonju.preprocess.worker.domain.artifact.dto.ArtifactUploadRequest;
import com.moonju.preprocess.worker.domain.artifact.dto.ArtifactUploadResult;
import com.moonju.preprocess.worker.domain.artifact.exception.ArtifactUploadFailedException;
import com.moonju.preprocess.worker.domain.artifact.model.ArtifactPath;
import com.moonju.preprocess.worker.domain.artifact.model.ArtifactType;
import com.moonju.preprocess.worker.domain.report.dto.ProcessingReport;
import com.moonju.preprocess.worker.domain.report.service.ProcessingReportWriter;
import org.springframework.stereotype.Service;

@Service
public class ProcessingReportSaveService {

    private final ArtifactSaveService artifactSaveService;
    private final ProcessingReportWriter processingReportWriter;

    public ProcessingReportSaveService(
        ArtifactSaveService artifactSaveService,
        ProcessingReportWriter processingReportWriter
    ) {
        this.artifactSaveService = artifactSaveService;
        this.processingReportWriter = processingReportWriter;
    }

    public ArtifactUploadResult save(Long projectId, Long jobId, Long itemId, ProcessingReport report) {
        ArtifactPath path = ArtifactPath.forType(ArtifactType.PROCESSING_REPORT, projectId, jobId, itemId);
        try {
            byte[] content = processingReportWriter.writeJsonBytes(report);
            return artifactSaveService.upload(
                ArtifactUploadRequest.upload(ArtifactType.PROCESSING_REPORT, path, content)
            );
        } catch (ArtifactUploadFailedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ArtifactUploadFailedException(
                "Processing report artifact save failed: " + path.value(),
                exception
            );
        }
    }
}
