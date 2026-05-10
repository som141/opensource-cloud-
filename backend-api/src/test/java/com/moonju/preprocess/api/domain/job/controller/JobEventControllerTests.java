package com.moonju.preprocess.api.domain.job.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.api.domain.job.service.JobEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class JobEventControllerTests {

    @Mock
    private JobEventService jobEventService;

    @Test
    void opensJobEventStream() {
        JobEventController controller = new JobEventController(jobEventService);
        SseEmitter emitter = new SseEmitter();
        when(jobEventService.subscribe(20L, 1L)).thenReturn(emitter);

        SseEmitter response = controller.events(20L, 1L);

        assertThat(response).isSameAs(emitter);
        verify(jobEventService).subscribe(20L, 1L);
    }
}
