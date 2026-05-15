package com.moonju.preprocess.api.domain.job.service;

import com.moonju.preprocess.api.domain.job.dto.JobArtifactResponse;
import com.moonju.preprocess.api.domain.job.dto.JobItemDownloadUrlResponse;
import com.moonju.preprocess.api.domain.job.dto.JobItemResponse;
import com.moonju.preprocess.api.domain.job.dto.JobResponse;
import com.moonju.preprocess.api.domain.job.dto.JobSummaryResponse;
import com.moonju.preprocess.api.domain.job.entity.Job;
import com.moonju.preprocess.api.domain.job.entity.JobItem;
import com.moonju.preprocess.api.domain.job.entity.JobItemArtifactDownloadType;
import com.moonju.preprocess.api.domain.job.exception.JobNotFoundException;
import com.moonju.preprocess.api.domain.job.repository.JobItemRepository;
import com.moonju.preprocess.api.domain.job.repository.JobRepository;
import com.moonju.preprocess.api.domain.project.service.ProjectPermissionService;
import com.moonju.preprocess.api.global.error.BusinessException;
import com.moonju.preprocess.api.global.error.ErrorCode;
import com.moonju.preprocess.api.global.response.PageResponse;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadCommand;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadTarget;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadUrlGenerator;
import java.time.Duration;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobQueryService {

    private static final Duration DOWNLOAD_URL_EXPIRES_IN = Duration.ofMinutes(10);

    private final JobRepository jobRepository;
    private final JobItemRepository jobItemRepository;
    private final ProjectPermissionService projectPermissionService;
    private final PresignedDownloadUrlGenerator presignedDownloadUrlGenerator;

    public JobQueryService(
        JobRepository jobRepository,
        JobItemRepository jobItemRepository,
        ProjectPermissionService projectPermissionService,
        PresignedDownloadUrlGenerator presignedDownloadUrlGenerator
    ) {
        this.jobRepository = jobRepository;
        this.jobItemRepository = jobItemRepository;
        this.projectPermissionService = projectPermissionService;
        this.presignedDownloadUrlGenerator = presignedDownloadUrlGenerator;
    }

    @Transactional(readOnly = true)
    public PageResponse<JobResponse> findMyJobs(Long currentUserId, Pageable pageable) {
        return PageResponse.from(jobRepository.findAllByUserId(currentUserId, pageable).map(JobResponse::from));
    }

    @Transactional(readOnly = true)
    public JobResponse findOne(Long currentUserId, Long jobId) {
        return JobResponse.from(findReadableJob(currentUserId, jobId));
    }

    @Transactional(readOnly = true)
    public PageResponse<JobItemResponse> findItems(Long currentUserId, Long jobId, Pageable pageable) {
        Job job = findReadableJob(currentUserId, jobId);
        return PageResponse.from(jobItemRepository.findAllByJobId(job.getId(), pageable).map(JobItemResponse::from));
    }

    @Transactional(readOnly = true)
    public JobSummaryResponse summary(Long currentUserId, Long jobId) {
        return JobSummaryResponse.from(findReadableJob(currentUserId, jobId));
    }

    @Transactional(readOnly = true)
    public JobArtifactResponse artifacts(Long currentUserId, Long jobId) {
        Job job = findReadableJob(currentUserId, jobId);
        return JobArtifactResponse.skeleton(job.getId());
    }

    @Transactional(readOnly = true)
    public JobItemDownloadUrlResponse createItemArtifactDownloadUrl(
        Long currentUserId,
        Long jobId,
        Long itemId,
        String typeValue
    ) {
        Job job = findReadableJob(currentUserId, jobId);
        JobItem item = jobItemRepository.findByIdAndJobId(itemId, job.getId())
            .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND, "Job item not found."));
        JobItemArtifactDownloadType type = parseItemArtifactType(typeValue);
        String objectKey = type.objectKey(item);
        if (objectKey == null || objectKey.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "Job item artifact is not ready.");
        }
        PresignedDownloadTarget target = presignedDownloadUrlGenerator.generateDownloadUrl(
            new PresignedDownloadCommand(objectKey, DOWNLOAD_URL_EXPIRES_IN)
        );
        return JobItemDownloadUrlResponse.of(job.getId(), item.getId(), type, target);
    }

    @Transactional(readOnly = true)
    public Job findReadableJob(Long currentUserId, Long jobId) {
        Job job = findById(jobId);
        projectPermissionService.validateReadable(job.getProjectId(), currentUserId);
        return job;
    }

    @Transactional(readOnly = true)
    public Job findEditableJob(Long currentUserId, Long jobId) {
        Job job = findById(jobId);
        projectPermissionService.validateEditable(job.getProjectId(), currentUserId);
        return job;
    }

    private Job findById(Long jobId) {
        return jobRepository.findById(jobId).orElseThrow(JobNotFoundException::new);
    }

    private JobItemArtifactDownloadType parseItemArtifactType(String typeValue) {
        try {
            return JobItemArtifactDownloadType.from(typeValue);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unsupported job item artifact type.");
        }
    }
}
