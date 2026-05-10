package com.moonju.preprocess.api.domain.job.service;

import com.moonju.preprocess.api.domain.job.dto.JobProgressEvent;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SseEmitterRegistry {

    public static final long DEFAULT_TIMEOUT_MILLIS = 30L * 60L * 1000L;

    private final Map<Long, Set<SseEmitter>> emittersByJobId = new ConcurrentHashMap<>();

    public SseEmitter register(Long jobId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT_MILLIS);
        emittersByJobId.computeIfAbsent(jobId, ignored -> ConcurrentHashMap.newKeySet()).add(emitter);
        emitter.onCompletion(() -> remove(jobId, emitter));
        emitter.onTimeout(() -> remove(jobId, emitter));
        emitter.onError(ignored -> remove(jobId, emitter));
        return emitter;
    }

    public int sendToJob(Long jobId, JobProgressEvent event) {
        Set<SseEmitter> emitters = emittersByJobId.getOrDefault(jobId, Set.of());
        int sentCount = 0;
        for (SseEmitter emitter : emitters) {
            if (send(jobId, emitter, event)) {
                sentCount++;
            }
        }
        return sentCount;
    }

    public int connectionCount(Long jobId) {
        return emittersByJobId.getOrDefault(jobId, Set.of()).size();
    }

    private boolean send(Long jobId, SseEmitter emitter, JobProgressEvent event) {
        try {
            emitter.send(SseEmitter.event()
                .name(event.eventType().name())
                .data(event));
            return true;
        } catch (IOException | IllegalStateException exception) {
            remove(jobId, emitter);
            return false;
        }
    }

    private void remove(Long jobId, SseEmitter emitter) {
        Set<SseEmitter> emitters = emittersByJobId.get(jobId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByJobId.remove(jobId);
        }
    }
}
