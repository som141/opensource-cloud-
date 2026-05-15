package com.moonju.preprocess.api.domain.job.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.api.domain.job.dto.JobZipDownloadResponse;
import com.moonju.preprocess.api.domain.job.entity.Job;
import com.moonju.preprocess.api.domain.job.entity.JobItem;
import com.moonju.preprocess.api.domain.job.entity.JobItemStatus;
import com.moonju.preprocess.api.domain.job.entity.JobPriority;
import com.moonju.preprocess.api.domain.job.repository.JobItemRepository;
import com.moonju.preprocess.api.domain.job.repository.JobRepository;
import com.moonju.preprocess.api.domain.project.service.ProjectPermissionService;
import com.moonju.preprocess.api.global.error.BusinessException;
import com.moonju.preprocess.api.infra.storage.ObjectStoragePort;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadCommand;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadTarget;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadUrlGenerator;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JobZipDownloadServiceTests {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobItemRepository jobItemRepository;

    @Mock
    private ProjectPermissionService projectPermissionService;

    @Mock
    private ObjectStoragePort objectStoragePort;

    @Mock
    private PresignedDownloadUrlGenerator presignedDownloadUrlGenerator;

    @InjectMocks
    private JobZipDownloadService service;

    @Test
    void createsProcessedZipAndReturnsDownloadUrl() throws Exception {
        Job job = job();
        JobItem first = succeededItem(2L, 100L, "processed/10/1/2/processed.png");
        JobItem second = succeededItem(3L, 101L, "processed/10/1/3/processed.webp");
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(jobItemRepository.findAllByJobIdAndStatusIn(eq(1L), any())).thenReturn(List.of(first, second));
        when(objectStoragePort.downloadBytes("processed/10/1/2/processed.png")).thenReturn("first".getBytes());
        when(objectStoragePort.downloadBytes("processed/10/1/3/processed.webp")).thenReturn("second".getBytes());
        when(presignedDownloadUrlGenerator.generateDownloadUrl(any(PresignedDownloadCommand.class)))
            .thenReturn(new PresignedDownloadTarget(
                "archives/10/1/processed-results.zip",
                "http://localhost/image-preprocess-local/archives/10/1/processed-results.zip",
                Instant.parse("2026-05-16T00:00:00Z"),
                Map.of()
            ));

        JobZipDownloadResponse response = service.createProcessedZip(20L, 1L);

        assertThat(response.fileCount()).isEqualTo(2);
        assertThat(response.objectKey()).isEqualTo("archives/10/1/processed-results.zip");
        verify(projectPermissionService).validateReadable(10L, 20L);

        ArgumentCaptor<byte[]> zipBytes = ArgumentCaptor.forClass(byte[].class);
        verify(objectStoragePort).uploadBytes(
            eq("archives/10/1/processed-results.zip"),
            zipBytes.capture(),
            eq("application/zip")
        );
        assertThat(zipEntryNames(zipBytes.getValue())).containsExactly(
            "image-100-item-2-processed.png",
            "image-101-item-3-processed.webp"
        );
    }

    @Test
    void rejectsZipDownloadWhenNoProcessedImagesAreReady() {
        Job job = job();
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(jobItemRepository.findAllByJobIdAndStatusIn(eq(1L), any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.createProcessedZip(20L, 1L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("No processed images are ready for ZIP download.");
        verify(objectStoragePort, never()).uploadBytes(any(), any(), any());
    }

    private Job job() {
        Job job = Job.create(10L, 20L, "A4_SCAN_300DPI", Map.of(), false, JobPriority.NORMAL, 2);
        ReflectionTestUtils.setField(job, "id", 1L);
        return job;
    }

    private JobItem succeededItem(Long id, Long imageId, String processedObjectKey) {
        JobItem item = new JobItem(1L, imageId, JobItemStatus.SUCCEEDED, 1);
        item.registerArtifacts(processedObjectKey, null, null);
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    private List<String> zipEntryNames(byte[] bytes) throws Exception {
        List<String> names = new ArrayList<>();
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            var entry = input.getNextEntry();
            while (entry != null) {
                names.add(entry.getName());
                entry = input.getNextEntry();
            }
        }
        return names;
    }
}
