package com.moonju.preprocess.api.domain.job.controller;

import com.moonju.preprocess.api.domain.job.dto.WorkerArtifactRegisterRequest;
import com.moonju.preprocess.api.domain.job.dto.WorkerArtifactRegisterResponse;
import com.moonju.preprocess.api.domain.job.dto.WorkerItemFailedRequest;
import com.moonju.preprocess.api.domain.job.dto.WorkerItemHeartbeatRequest;
import com.moonju.preprocess.api.domain.job.dto.WorkerItemReportResponse;
import com.moonju.preprocess.api.domain.job.dto.WorkerItemStartedRequest;
import com.moonju.preprocess.api.domain.job.dto.WorkerItemSucceededRequest;
import com.moonju.preprocess.api.domain.job.service.InternalWorkerJobService;
import com.moonju.preprocess.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Hidden
@RequestMapping("/internal/v1/jobs/{jobId}/items/{itemId}")
public class InternalWorkerJobController {

    private final InternalWorkerJobService workerJobService;

    public InternalWorkerJobController(InternalWorkerJobService workerJobService) {
        this.workerJobService = workerJobService;
    }

    @PostMapping("/started")
    public ApiResponse<WorkerItemReportResponse> started(
        @PathVariable Long jobId,
        @PathVariable Long itemId,
        @Valid @RequestBody WorkerItemStartedRequest request
    ) {
        return ApiResponse.success(workerJobService.started(jobId, itemId, request));
    }

    @PostMapping("/heartbeat")
    public ApiResponse<WorkerItemReportResponse> heartbeat(
        @PathVariable Long jobId,
        @PathVariable Long itemId,
        @Valid @RequestBody WorkerItemHeartbeatRequest request
    ) {
        return ApiResponse.success(workerJobService.heartbeat(jobId, itemId, request));
    }

    @PostMapping("/succeeded")
    public ApiResponse<WorkerItemReportResponse> succeeded(
        @PathVariable Long jobId,
        @PathVariable Long itemId,
        @Valid @RequestBody WorkerItemSucceededRequest request
    ) {
        return ApiResponse.success(workerJobService.succeeded(jobId, itemId, request));
    }

    @PostMapping("/failed")
    public ApiResponse<WorkerItemReportResponse> failed(
        @PathVariable Long jobId,
        @PathVariable Long itemId,
        @Valid @RequestBody WorkerItemFailedRequest request
    ) {
        return ApiResponse.success(workerJobService.failed(jobId, itemId, request));
    }

    @PostMapping("/artifacts")
    public ApiResponse<WorkerArtifactRegisterResponse> artifacts(
        @PathVariable Long jobId,
        @PathVariable Long itemId,
        @Valid @RequestBody WorkerArtifactRegisterRequest request
    ) {
        return ApiResponse.success(workerJobService.artifacts(jobId, itemId, request));
    }
}
