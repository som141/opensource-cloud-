package com.moonju.preprocess.api.domain.job.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.api.domain.job.dto.JobSummaryResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class JobEventServiceTests {

    private final SseEmitterRegistry emitterRegistry = new SseEmitterRegistry();
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-10T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private JobQueryService jobQueryService;

    @Test
    void subscribesAfterValidatingReadableJobAndSendsInitialEvents() {
        JobEventService service = new JobEventService(jobQueryService, emitterRegistry, clock);
        JobSummaryResponse summary = new JobSummaryResponse(1L, 10, 2, 1, 6, 1, 70.0);
        when(jobQueryService.summary(20L, 1L)).thenReturn(summary);

        SseEmitter emitter = service.subscribe(20L, 1L);

        assertThat(emitter).isNotNull();
        assertThat(service.connectionCount(1L)).isEqualTo(1);
        verify(jobQueryService).summary(20L, 1L);
    }

    @Test
    void publishesProgressCompletedFailedAndHeartbeatToRegisteredEmitters() {
        JobEventService service = new JobEventService(jobQueryService, emitterRegistry, clock);
        JobSummaryResponse summary = new JobSummaryResponse(1L, 10, 0, 0, 10, 0, 100.0);
        when(jobQueryService.summary(20L, 1L)).thenReturn(summary);
        service.subscribe(20L, 1L);

        assertThat(service.publishProgress(summary)).isEqualTo(1);
        assertThat(service.publishCompleted(summary)).isEqualTo(1);
        assertThat(service.publishFailed(summary)).isEqualTo(1);
        assertThat(service.publishHeartbeat(1L)).isEqualTo(1);
    }
}
