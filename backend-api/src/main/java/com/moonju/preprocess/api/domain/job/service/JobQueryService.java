package com.moonju.preprocess.api.domain.job.service;

import com.moonju.preprocess.api.domain.job.dto.JobArtifactResponse;
import com.moonju.preprocess.api.domain.job.dto.JobItemDownloadUrlResponse;
import com.moonju.preprocess.api.domain.job.dto.JobItemResponse;
import com.moonju.preprocess.api.domain.job.dto.JobResponse;
import com.moonju.preprocess.api.domain.job.dto.JobSummaryResponse;
import com.moonju.preprocess.api.domain.job.entity.Job;
import com.moonju.preprocess.api.domain.job.entity.JobItem;
import com.moonju.preprocess.api.domain.job.entity.JobItemArtifactType;
import com.moonju.preprocess.api.domain.job.exception.JobItemArtifactNotFoundException;
import com.moonju.preprocess.api.domain.job.exception.JobItemNotFoundException;
import com.moonju.preprocess.api.domain.job.exception.JobNotFoundException;
import com.moonju.preprocess.api.domain.job.repository.JobItemRepository;
import com.moonju.preprocess.api.domain.job.repository.JobRepository;
import com.moonju.preprocess.api.domain.project.service.ProjectPermissionService;
import com.moonju.preprocess.api.global.response.PageResponse;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadCommand;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadTarget;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadUrlGenerator;
import java.time.Duration;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class JobQueryService {

    private static final Duration ARTIFACT_DOWNLOAD_URL_EXPIRES_IN = Duration.ofMinutes(10);

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
    public JobItemDownloadUrlResponse createItemDownloadUrl(
        Long currentUserId,
        Long jobId,
        Long itemId,
        String typeValue
    ) {
        Job job = findReadableJob(currentUserId, jobId);
        JobItem item = jobItemRepository.findByIdAndJobId(itemId, job.getId())
            .orElseThrow(JobItemNotFoundException::new);
        JobItemArtifactType type = JobItemArtifactType.from(typeValue);
        String objectKey = type.objectKey(item);
        if (!StringUtils.hasText(objectKey)) {
            throw new JobItemArtifactNotFoundException();
        }
        PresignedDownloadTarget target = presignedDownloadUrlGenerator.generateDownloadUrl(
            new PresignedDownloadCommand(objectKey, ARTIFACT_DOWNLOAD_URL_EXPIRES_IN)
        );
        return JobItemDownloadUrlResponse.of(job.getId(), item.getId(), type.apiValue(), target);
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
}
