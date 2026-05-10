package com.moonju.preprocess.api.domain.job.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.api.domain.job.dto.JobCancelResponse;
import com.moonju.preprocess.api.domain.job.entity.Job;
import com.moonju.preprocess.api.domain.job.entity.JobItem;
import com.moonju.preprocess.api.domain.job.entity.JobItemStatus;
import com.moonju.preprocess.api.domain.job.entity.JobPriority;
import com.moonju.preprocess.api.domain.job.entity.JobStatus;
import com.moonju.preprocess.api.domain.job.repository.JobItemRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JobCancelServiceTests {

    @Mock
    private JobQueryService jobQueryService;

    @Mock
    private JobItemRepository jobItemRepository;

    @Test
    void marksJobAndQueuedItemsAsCancelRequested() {
        JobCancelService service = new JobCancelService(jobQueryService, jobItemRepository);
        Job job = job(1L);
        JobItem queuedItem = JobItem.queued(1L, 100L);
        JobItem processingItem = new JobItem(1L, 101L, JobItemStatus.PROCESSING, 1);
        when(jobQueryService.findEditableJob(20L, 1L)).thenReturn(job);
        when(jobItemRepository.findAllByJobId(1L)).thenReturn(List.of(queuedItem, processingItem));

        JobCancelResponse response = service.cancel(20L, 1L);

        assertThat(response.status()).isEqualTo(JobStatus.CANCEL_REQUESTED);
        assertThat(queuedItem.getStatus()).isEqualTo(JobItemStatus.CANCELLED);
        assertThat(processingItem.getStatus()).isEqualTo(JobItemStatus.PROCESSING);
    }

    private Job job(Long id) {
        Job job = Job.create(10L, 20L, "A4_SCAN_300DPI", Map.of(), false, JobPriority.NORMAL, 2);
        ReflectionTestUtils.setField(job, "id", id);
        job.markQueued(2);
        return job;
    }
}
