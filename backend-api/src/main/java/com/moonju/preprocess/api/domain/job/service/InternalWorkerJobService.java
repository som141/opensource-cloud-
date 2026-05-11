package com.moonju.preprocess.api.domain.job.service;

import com.moonju.preprocess.api.domain.job.dto.JobSummaryResponse;
import com.moonju.preprocess.api.domain.job.dto.WorkerArtifactRegisterRequest;
import com.moonju.preprocess.api.domain.job.dto.WorkerArtifactRegisterResponse;
import com.moonju.preprocess.api.domain.job.dto.WorkerItemFailedRequest;
import com.moonju.preprocess.api.domain.job.dto.WorkerItemHeartbeatRequest;
import com.moonju.preprocess.api.domain.job.dto.WorkerItemReportResponse;
import com.moonju.preprocess.api.domain.job.dto.WorkerItemStartedRequest;
import com.moonju.preprocess.api.domain.job.dto.WorkerItemSucceededRequest;
import com.moonju.preprocess.api.domain.job.entity.Job;
import com.moonju.preprocess.api.domain.job.entity.JobItem;
import com.moonju.preprocess.api.domain.job.entity.JobStatus;
import com.moonju.preprocess.api.domain.job.exception.InvalidWorkerReportException;
import com.moonju.preprocess.api.domain.job.exception.JobNotFoundException;
import com.moonju.preprocess.api.domain.job.repository.JobItemRepository;
import com.moonju.preprocess.api.domain.job.repository.JobRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InternalWorkerJobService {

    private final JobRepository jobRepository;
    private final JobItemRepository jobItemRepository;
    private final JobEventService jobEventService;
    private final Clock clock;

    @Autowired
    public InternalWorkerJobService(
        JobRepository jobRepository,
        JobItemRepository jobItemRepository,
        JobEventService jobEventService
    ) {
        this(jobRepository, jobItemRepository, jobEventService, Clock.systemUTC());
    }

    InternalWorkerJobService(
        JobRepository jobRepository,
        JobItemRepository jobItemRepository,
        JobEventService jobEventService,
        Clock clock
    ) {
        this.jobRepository = jobRepository;
        this.jobItemRepository = jobItemRepository;
        this.jobEventService = jobEventService;
        this.clock = clock;
    }

    @Transactional
    public WorkerItemReportResponse started(Long jobId, Long itemId, WorkerItemStartedRequest request) {
        Job job = findJob(jobId);
        JobItem item = findItem(jobId, itemId);
        if (!item.canStartProcessing() && !item.isProcessing()) {
            throw new InvalidWorkerReportException("Only queued, pending, retrying, or processing items can start.");
        }

        LocalDateTime now = now();
        item.markProcessing(request.workerId(), request.attempt(), now);
        refreshProgressAndPublish(job, now);
        return response(jobId, itemId, item, now);
    }

    @Transactional
    public WorkerItemReportResponse heartbeat(Long jobId, Long itemId, WorkerItemHeartbeatRequest request) {
        Job job = findJob(jobId);
        JobItem item = findItem(jobId, itemId);
        if (!item.isProcessing()) {
            throw new InvalidWorkerReportException("Only processing items can receive heartbeat reports.");
        }

        LocalDateTime now = now();
        item.markHeartbeat(request.workerId(), now);
        refreshProgressAndPublish(job, now);
        jobEventService.publishHeartbeat(jobId);
        return response(jobId, itemId, item, now);
    }

    @Transactional
    public WorkerItemReportResponse succeeded(Long jobId, Long itemId, WorkerItemSucceededRequest request) {
        Job job = findJob(jobId);
        JobItem item = findItem(jobId, itemId);
        if (!item.isProcessing()) {
            throw new InvalidWorkerReportException("Only processing items can succeed.");
        }

        LocalDateTime now = now();
        item.markSucceeded(
            request.workerId(),
            request.processedObjectKey(),
            request.previewObjectKey(),
            request.reportObjectKey(),
            now
        );
        refreshProgressAndPublish(job, now);
        return response(jobId, itemId, item, now);
    }

    @Transactional
    public WorkerItemReportResponse failed(Long jobId, Long itemId, WorkerItemFailedRequest request) {
        Job job = findJob(jobId);
        JobItem item = findItem(jobId, itemId);
        if (!item.isProcessing()) {
            throw new InvalidWorkerReportException("Only processing items can fail.");
        }

        LocalDateTime now = now();
        item.markFailed(request.workerId(), request.errorCode(), request.errorMessage(), now);
        refreshProgressAndPublish(job, now);
        return response(jobId, itemId, item, now);
    }

    @Transactional
    public WorkerArtifactRegisterResponse artifacts(
        Long jobId,
        Long itemId,
        WorkerArtifactRegisterRequest request
    ) {
        findJob(jobId);
        JobItem item = findItem(jobId, itemId);
        if (!item.canRegisterArtifacts()) {
            throw new InvalidWorkerReportException("Artifacts can be registered only for processing or succeeded items.");
        }

        LocalDateTime now = now();
        item.registerArtifacts(request.processedObjectKey(), request.previewObjectKey(), request.reportObjectKey());
        return new WorkerArtifactRegisterResponse(jobId, itemId, true, now);
    }

    private Job findJob(Long jobId) {
        return jobRepository.findById(jobId).orElseThrow(JobNotFoundException::new);
    }

    private JobItem findItem(Long jobId, Long itemId) {
        return jobItemRepository.findByIdAndJobId(itemId, jobId).orElseThrow(JobNotFoundException::new);
    }

    private void refreshProgressAndPublish(Job job, LocalDateTime now) {
        List<JobItem> items = jobItemRepository.findAllByJobId(job.getId());
        job.refreshProgress(items, now);
        JobSummaryResponse summary = JobSummaryResponse.from(job);
        if (job.getStatus() == JobStatus.SUCCEEDED) {
            jobEventService.publishCompleted(summary);
            return;
        }
        if (job.getStatus() == JobStatus.FAILED) {
            jobEventService.publishFailed(summary);
            return;
        }
        jobEventService.publishProgress(summary);
    }

    private WorkerItemReportResponse response(Long jobId, Long itemId, JobItem item, LocalDateTime reportedAt) {
        return new WorkerItemReportResponse(jobId, itemId, item.getStatus(), item.getWorkerId(), reportedAt);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
