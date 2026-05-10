package com.moonju.preprocess.api.domain.job.service;

import com.moonju.preprocess.api.domain.job.dto.JobCancelResponse;
import com.moonju.preprocess.api.domain.job.entity.Job;
import com.moonju.preprocess.api.domain.job.repository.JobItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobCancelService {

    private final JobQueryService jobQueryService;
    private final JobItemRepository jobItemRepository;

    public JobCancelService(JobQueryService jobQueryService, JobItemRepository jobItemRepository) {
        this.jobQueryService = jobQueryService;
        this.jobItemRepository = jobItemRepository;
    }

    @Transactional
    public JobCancelResponse cancel(Long currentUserId, Long jobId) {
        Job job = jobQueryService.findEditableJob(currentUserId, jobId);
        job.requestCancel();
        jobItemRepository.findAllByJobId(job.getId()).forEach(item -> {
            item.requestCancel();
        });
        return JobCancelResponse.from(job);
    }
}
