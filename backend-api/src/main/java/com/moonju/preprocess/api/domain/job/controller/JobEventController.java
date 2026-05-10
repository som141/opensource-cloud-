package com.moonju.preprocess.api.domain.job.controller;

import com.moonju.preprocess.api.domain.job.service.JobEventService;
import com.moonju.preprocess.api.global.support.CurrentUser;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobEventController {

    private final JobEventService jobEventService;

    public JobEventController(JobEventService jobEventService) {
        this.jobEventService = jobEventService;
    }

    @GetMapping(value = "/{jobId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@CurrentUser Long currentUserId, @PathVariable Long jobId) {
        return jobEventService.subscribe(currentUserId, jobId);
    }
}
