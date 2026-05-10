package com.moonju.preprocess.api.domain.job.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.api.domain.job.dto.JobSummaryResponse;
import com.moonju.preprocess.api.domain.job.dto.WorkerArtifactRegisterRequest;
import com.moonju.preprocess.api.domain.job.dto.WorkerItemFailedRequest;
import com.moonju.preprocess.api.domain.job.dto.WorkerItemHeartbeatRequest;
import com.moonju.preprocess.api.domain.job.dto.WorkerItemReportResponse;
import com.moonju.preprocess.api.domain.job.dto.WorkerItemStartedRequest;
import com.moonju.preprocess.api.domain.job.dto.WorkerItemSucceededRequest;
import com.moonju.preprocess.api.domain.job.entity.Job;
import com.moonju.preprocess.api.domain.job.entity.JobItem;
import com.moonju.preprocess.api.domain.job.entity.JobItemStatus;
import com.moonju.preprocess.api.domain.job.entity.JobPriority;
import com.moonju.preprocess.api.domain.job.entity.JobStatus;
import com.moonju.preprocess.api.domain.job.exception.InvalidWorkerReportException;
import com.moonju.preprocess.api.domain.job.repository.JobItemRepository;
import com.moonju.preprocess.api.domain.job.repository.JobRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InternalWorkerJobServiceTests {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-10T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobItemRepository jobItemRepository;

    @Mock
    private JobEventService jobEventService;

    @Test
    void marksItemStartedAndPublishesProgress() {
        Job job = job();
        JobItem item = item(JobItemStatus.QUEUED);
        stubJobAndItem(job, item);
        when(jobItemRepository.findAllByJobId(1L)).thenReturn(List.of(item));
        InternalWorkerJobService service = service();

        WorkerItemReportResponse response = service.started(1L, 10L, new WorkerItemStartedRequest("worker-1", 1));

        assertThat(response.status()).isEqualTo(JobItemStatus.PROCESSING);
        assertThat(item.getWorkerId()).isEqualTo("worker-1");
        assertThat(job.getStatus()).isEqualTo(JobStatus.RUNNING);
        verify(jobEventService).publishProgress(any(JobSummaryResponse.class));
    }

    @Test
    void storesHeartbeatOnlyForProcessingItem() {
        Job job = job();
        JobItem item = item(JobItemStatus.PROCESSING);
        stubJobAndItem(job, item);
        when(jobItemRepository.findAllByJobId(1L)).thenReturn(List.of(item));
        InternalWorkerJobService service = service();

        WorkerItemReportResponse response = service.heartbeat(1L, 10L, new WorkerItemHeartbeatRequest("worker-1"));

        assertThat(response.workerId()).isEqualTo("worker-1");
        assertThat(item.getLastHeartbeatAt()).isNotNull();
        verify(jobEventService).publishHeartbeat(1L);
    }

    @Test
    void marksItemSucceededAndPublishesCompletedWhenAllItemsSucceeded() {
        Job job = job();
        JobItem item = item(JobItemStatus.PROCESSING);
        stubJobAndItem(job, item);
        when(jobItemRepository.findAllByJobId(1L)).thenReturn(List.of(item));
        InternalWorkerJobService service = service();

        WorkerItemReportResponse response = service.succeeded(
            1L,
            10L,
            new WorkerItemSucceededRequest("worker-1", "processed/key.png", "preview/key.png", "report/key.json")
        );

        assertThat(response.status()).isEqualTo(JobItemStatus.SUCCEEDED);
        assertThat(item.getProcessedObjectKey()).isEqualTo("processed/key.png");
        assertThat(job.getStatus()).isEqualTo(JobStatus.SUCCEEDED);
        verify(jobEventService).publishCompleted(any(JobSummaryResponse.class));
    }

    @Test
    void marksItemFailedAndPublishesFailedWhenAllItemsFailed() {
        Job job = job();
        JobItem item = item(JobItemStatus.PROCESSING);
        stubJobAndItem(job, item);
        when(jobItemRepository.findAllByJobId(1L)).thenReturn(List.of(item));
        InternalWorkerJobService service = service();

        WorkerItemReportResponse response = service.failed(
            1L,
            10L,
            new WorkerItemFailedRequest("worker-1", "DECODE_FAILED", "cannot decode", false)
        );

        assertThat(response.status()).isEqualTo(JobItemStatus.FAILED);
        assertThat(item.getErrorCode()).isEqualTo("DECODE_FAILED");
        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        verify(jobEventService).publishFailed(any(JobSummaryResponse.class));
    }

    @Test
    void registersArtifactsForSucceededItem() {
        Job job = job();
        JobItem item = item(JobItemStatus.SUCCEEDED);
        stubJobAndItem(job, item);
        InternalWorkerJobService service = service();

        service.artifacts(1L, 10L, new WorkerArtifactRegisterRequest("processed/key.png", null, "report/key.json"));

        assertThat(item.getProcessedObjectKey()).isEqualTo("processed/key.png");
        assertThat(item.getReportObjectKey()).isEqualTo("report/key.json");
    }

    @Test
    void rejectsSucceededReportForQueuedItem() {
        Job job = job();
        JobItem item = item(JobItemStatus.QUEUED);
        stubJobAndItem(job, item);
        InternalWorkerJobService service = service();

        assertThatThrownBy(() -> service.succeeded(
            1L,
            10L,
            new WorkerItemSucceededRequest("worker-1", "processed/key.png", null, "report/key.json")
        )).isInstanceOf(InvalidWorkerReportException.class);
    }

    private InternalWorkerJobService service() {
        return new InternalWorkerJobService(jobRepository, jobItemRepository, jobEventService, clock);
    }

    private Job job() {
        Job job = Job.create(1L, 2L, "A4_SCAN_300DPI", Map.of(), false, JobPriority.NORMAL, 1);
        ReflectionTestUtils.setField(job, "id", 1L);
        return job;
    }

    private JobItem item(JobItemStatus status) {
        JobItem item = new JobItem(1L, 100L, status, 1);
        ReflectionTestUtils.setField(item, "id", 10L);
        if (status == JobItemStatus.PROCESSING || status == JobItemStatus.SUCCEEDED) {
            item.markProcessing("worker-1", 1, java.time.LocalDateTime.of(2026, 5, 10, 0, 0));
        }
        if (status == JobItemStatus.SUCCEEDED) {
            item.markSucceeded(
                "worker-1",
                "processed/old.png",
                null,
                "report/old.json",
                java.time.LocalDateTime.of(2026, 5, 10, 0, 1)
            );
        }
        return item;
    }

    private void stubJobAndItem(Job job, JobItem item) {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(jobItemRepository.findByIdAndJobId(10L, 1L)).thenReturn(Optional.of(item));
    }
}
