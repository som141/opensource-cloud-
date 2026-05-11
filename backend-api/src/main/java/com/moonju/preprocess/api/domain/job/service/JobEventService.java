package com.moonju.preprocess.api.domain.job.service;

import com.moonju.preprocess.api.domain.job.dto.JobEventType;
import com.moonju.preprocess.api.domain.job.dto.JobProgressEvent;
import com.moonju.preprocess.api.domain.job.dto.JobSummaryResponse;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class JobEventService {

    private final JobQueryService jobQueryService;
    private final SseEmitterRegistry emitterRegistry;
    private final Clock clock;

    @Autowired
    public JobEventService(JobQueryService jobQueryService, SseEmitterRegistry emitterRegistry) {
        this(jobQueryService, emitterRegistry, Clock.systemUTC());
    }

    JobEventService(JobQueryService jobQueryService, SseEmitterRegistry emitterRegistry, Clock clock) {
        this.jobQueryService = jobQueryService;
        this.emitterRegistry = emitterRegistry;
        this.clock = clock;
    }

    public SseEmitter subscribe(Long currentUserId, Long jobId) {
        JobSummaryResponse summary = jobQueryService.summary(currentUserId, jobId);
        SseEmitter emitter = emitterRegistry.register(jobId);
        sendInitialEvents(emitter, summary);
        return emitter;
    }

    public int publishProgress(JobSummaryResponse summary) {
        return emitterRegistry.sendToJob(
            summary.jobId(),
            JobProgressEvent.fromSummary(JobEventType.JOB_PROGRESS, summary, clock.instant())
        );
    }

    public int publishCompleted(JobSummaryResponse summary) {
        return emitterRegistry.sendToJob(
            summary.jobId(),
            JobProgressEvent.fromSummary(JobEventType.JOB_COMPLETED, summary, clock.instant())
        );
    }

    public int publishFailed(JobSummaryResponse summary) {
        return emitterRegistry.sendToJob(
            summary.jobId(),
            JobProgressEvent.fromSummary(JobEventType.JOB_FAILED, summary, clock.instant())
        );
    }

    public int publishHeartbeat(Long jobId) {
        return emitterRegistry.sendToJob(jobId, JobProgressEvent.heartbeat(jobId, clock.instant()));
    }

    public int connectionCount(Long jobId) {
        return emitterRegistry.connectionCount(jobId);
    }

    private void sendInitialEvents(SseEmitter emitter, JobSummaryResponse summary) {
        try {
            emitter.send(SseEmitter.event()
                .name(JobEventType.HEARTBEAT.name())
                .data(JobProgressEvent.heartbeat(summary.jobId(), clock.instant())));
            emitter.send(SseEmitter.event()
                .name(JobEventType.JOB_PROGRESS.name())
                .data(JobProgressEvent.fromSummary(JobEventType.JOB_PROGRESS, summary, clock.instant())));
        } catch (Exception exception) {
            emitter.completeWithError(exception);
        }
    }
}
