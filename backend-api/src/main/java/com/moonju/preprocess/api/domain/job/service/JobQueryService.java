package com.moonju.preprocess.api.domain.job.service;

import com.moonju.preprocess.api.domain.job.dto.JobArtifactResponse;
import com.moonju.preprocess.api.domain.job.dto.JobItemResponse;
import com.moonju.preprocess.api.domain.job.dto.JobResponse;
import com.moonju.preprocess.api.domain.job.dto.JobSummaryResponse;
import com.moonju.preprocess.api.domain.job.entity.Job;
import com.moonju.preprocess.api.domain.job.exception.JobNotFoundException;
import com.moonju.preprocess.api.domain.job.repository.JobItemRepository;
import com.moonju.preprocess.api.domain.job.repository.JobRepository;
import com.moonju.preprocess.api.domain.project.service.ProjectPermissionService;
import com.moonju.preprocess.api.global.response.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobQueryService {

    private final JobRepository jobRepository;
    private final JobItemRepository jobItemRepository;
    private final ProjectPermissionService projectPermissionService;

    public JobQueryService(
        JobRepository jobRepository,
        JobItemRepository jobItemRepository,
        ProjectPermissionService projectPermissionService
    ) {
        this.jobRepository = jobRepository;
        this.jobItemRepository = jobItemRepository;
        this.projectPermissionService = projectPermissionService;
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
