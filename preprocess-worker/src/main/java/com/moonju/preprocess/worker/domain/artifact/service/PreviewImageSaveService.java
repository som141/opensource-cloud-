package com.moonju.preprocess.worker.domain.artifact.service;

import com.moonju.preprocess.worker.domain.artifact.dto.ArtifactUploadRequest;
import com.moonju.preprocess.worker.domain.artifact.dto.ArtifactUploadResult;
import com.moonju.preprocess.worker.domain.artifact.exception.ArtifactUploadFailedException;
import com.moonju.preprocess.worker.domain.artifact.model.ArtifactPath;
import com.moonju.preprocess.worker.domain.artifact.model.ArtifactType;
import com.moonju.preprocess.worker.domain.preprocess.model.ImageMatHolder;
import com.moonju.preprocess.worker.domain.preprocess.service.ImageEncodePort;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;

@Service
public class PreviewImageSaveService {

    private static final int PREVIEW_MAX_DIMENSION = 1024;

    private final ArtifactSaveService artifactSaveService;
    private final ImageEncodePort imageEncodePort;

    public PreviewImageSaveService(ArtifactSaveService artifactSaveService, ImageEncodePort imageEncodePort) {
        this.artifactSaveService = artifactSaveService;
        this.imageEncodePort = imageEncodePort;
    }

    public ArtifactUploadResult prepare(Long projectId, Long jobId, Long itemId) {
        ArtifactPath path = ArtifactPath.forType(ArtifactType.PREVIEW_IMAGE, projectId, jobId, itemId);
        return artifactSaveService.prepareUpload(ArtifactUploadRequest.skeleton(ArtifactType.PREVIEW_IMAGE, path));
    }

    public ArtifactUploadResult save(Long projectId, Long jobId, Long itemId, ImageMatHolder processedImage) {
        ArtifactPath path = ArtifactPath.forType(ArtifactType.PREVIEW_IMAGE, projectId, jobId, itemId);
        Mat preview = previewMat(processedImage);
        try {
            byte[] content = imageEncodePort.encodePng(path.value(), preview);
            return artifactSaveService.upload(ArtifactUploadRequest.upload(ArtifactType.PREVIEW_IMAGE, path, content));
        } catch (ArtifactUploadFailedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ArtifactUploadFailedException("Preview image artifact save failed: " + path.value(), exception);
        } finally {
            preview.release();
        }
    }

    private Mat previewMat(ImageMatHolder processedImage) {
        Mat source = processedImage.mat();
        int maxDimension = Math.max(processedImage.width(), processedImage.height());
        Mat preview = new Mat();
        if (maxDimension <= PREVIEW_MAX_DIMENSION) {
            source.copyTo(preview);
            return preview;
        }

        double scale = PREVIEW_MAX_DIMENSION / (double) maxDimension;
        Size size = new Size(
            Math.max(1, Math.round(processedImage.width() * scale)),
            Math.max(1, Math.round(processedImage.height() * scale))
        );
        Imgproc.resize(source, preview, size, 0, 0, Imgproc.INTER_AREA);
        return preview;
    }
}
