package com.moonju.preprocess.worker.domain.workerjob.listener;

import static org.mockito.Mockito.verify;

import com.moonju.preprocess.worker.domain.workerjob.dto.JobPriority;
import com.moonju.preprocess.worker.domain.workerjob.dto.PreprocessJobMessage;
import com.moonju.preprocess.worker.domain.workerjob.service.WorkerJobService;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PreprocessJobListenerTests {

    @Test
    void delegatesMessageToWorkerJobService() {
        WorkerJobService service = org.mockito.Mockito.mock(WorkerJobService.class);
        PreprocessJobListener listener = new PreprocessJobListener(service);
        PreprocessJobMessage message = message();

        listener.handle(message);

        verify(service).process(message);
    }

    private PreprocessJobMessage message() {
        return new PreprocessJobMessage(
            "msg",
            1L,
            2L,
            3L,
            4L,
            5L,
            "originals/scan.png",
            "A4_SCAN_300DPI",
            Map.of(),
            false,
            JobPriority.NORMAL,
            1,
            "trace",
            Instant.parse("2026-05-10T00:00:00Z")
        );
    }
}
