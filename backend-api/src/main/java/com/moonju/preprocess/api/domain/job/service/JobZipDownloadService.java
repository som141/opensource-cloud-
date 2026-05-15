package com.moonju.preprocess.api.domain.job.service;

import com.moonju.preprocess.api.domain.job.dto.JobZipDownloadResponse;
import com.moonju.preprocess.api.domain.job.entity.Job;
import com.moonju.preprocess.api.domain.job.entity.JobItem;
import com.moonju.preprocess.api.domain.job.entity.JobItemStatus;
import com.moonju.preprocess.api.domain.job.exception.JobNotFoundException;
import com.moonju.preprocess.api.domain.job.repository.JobItemRepository;
import com.moonju.preprocess.api.domain.job.repository.JobRepository;
import com.moonju.preprocess.api.domain.project.service.ProjectPermissionService;
import com.moonju.preprocess.api.global.error.BusinessException;
import com.moonju.preprocess.api.global.error.ErrorCode;
import com.moonju.preprocess.api.infra.storage.ObjectStoragePort;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadCommand;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadTarget;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadUrlGenerator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;

@Service
public class JobZipDownloadService {

    private static final Duration DOWNLOAD_URL_EXPIRES_IN = Duration.ofMinutes(10);
    private static final String ZIP_CONTENT_TYPE = "application/zip";

    private final JobRepository jobRepository;
    private final JobItemRepository jobItemRepository;
    private final ProjectPermissionService projectPermissionService;
    private final ObjectStoragePort objectStoragePort;
    private final PresignedDownloadUrlGenerator presignedDownloadUrlGenerator;

    public JobZipDownloadService(
        JobRepository jobRepository,
        JobItemRepository jobItemRepository,
        ProjectPermissionService projectPermissionService,
        ObjectStoragePort objectStoragePort,
        PresignedDownloadUrlGenerator presignedDownloadUrlGenerator
    ) {
        this.jobRepository = jobRepository;
        this.jobItemRepository = jobItemRepository;
        this.projectPermissionService = projectPermissionService;
        this.objectStoragePort = objectStoragePort;
        this.presignedDownloadUrlGenerator = presignedDownloadUrlGenerator;
    }

    public JobZipDownloadResponse createProcessedZip(Long currentUserId, Long jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow(JobNotFoundException::new);
        projectPermissionService.validateReadable(job.getProjectId(), currentUserId);

        List<JobItem> readyItems = jobItemRepository
            .findAllByJobIdAndStatusIn(job.getId(), Set.of(JobItemStatus.SUCCEEDED))
            .stream()
            .filter(item -> item.getProcessedObjectKey() != null && !item.getProcessedObjectKey().isBlank())
            .toList();
        if (readyItems.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "No processed images are ready for ZIP download.");
        }

        byte[] zipBytes = createZipBytes(readyItems);
        String archiveObjectKey = archiveObjectKey(job);
        objectStoragePort.uploadBytes(archiveObjectKey, zipBytes, ZIP_CONTENT_TYPE);
        PresignedDownloadTarget target = presignedDownloadUrlGenerator.generateDownloadUrl(
            new PresignedDownloadCommand(archiveObjectKey, DOWNLOAD_URL_EXPIRES_IN)
        );
        return JobZipDownloadResponse.of(job.getId(), readyItems.size(), target);
    }

    private byte[] createZipBytes(List<JobItem> items) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             ZipOutputStream zipStream = new ZipOutputStream(byteStream)) {
            for (JobItem item : items) {
                zipStream.putNextEntry(new ZipEntry(entryName(item)));
                zipStream.write(objectStoragePort.downloadBytes(item.getProcessedObjectKey()));
                zipStream.closeEntry();
            }
            zipStream.finish();
            return byteStream.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_SERVER_ERROR, "Failed to create processed ZIP.");
        }
    }

    private String archiveObjectKey(Job job) {
        return "archives/%d/%d/processed-results.zip".formatted(job.getProjectId(), job.getId());
    }

    private String entryName(JobItem item) {
        return "image-%d-item-%d-processed%s".formatted(
            item.getImageId(),
            item.getId(),
            extension(item.getProcessedObjectKey())
        );
    }

    private String extension(String objectKey) {
        int lastSlash = objectKey.lastIndexOf('/');
        int lastDot = objectKey.lastIndexOf('.');
        if (lastDot <= lastSlash || lastDot == objectKey.length() - 1) {
            return ".png";
        }
        String extension = objectKey.substring(lastDot).toLowerCase(Locale.ROOT);
        return extension.length() > 12 ? ".png" : extension;
    }
}
