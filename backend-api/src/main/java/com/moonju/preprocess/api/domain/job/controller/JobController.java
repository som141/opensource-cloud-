package com.moonju.preprocess.api.domain.job.controller;

import com.moonju.preprocess.api.domain.job.dto.JobArtifactResponse;
import com.moonju.preprocess.api.domain.job.dto.JobCancelResponse;
import com.moonju.preprocess.api.domain.job.dto.JobCreateRequest;
import com.moonju.preprocess.api.domain.job.dto.JobCreateResponse;
import com.moonju.preprocess.api.domain.job.dto.JobItemDownloadUrlResponse;
import com.moonju.preprocess.api.domain.job.dto.JobItemResponse;
import com.moonju.preprocess.api.domain.job.dto.JobResponse;
import com.moonju.preprocess.api.domain.job.dto.JobRetryResponse;
import com.moonju.preprocess.api.domain.job.dto.JobSummaryResponse;
import com.moonju.preprocess.api.domain.job.service.JobCancelService;
import com.moonju.preprocess.api.domain.job.service.JobCommandService;
import com.moonju.preprocess.api.domain.job.service.JobQueryService;
import com.moonju.preprocess.api.domain.job.service.JobRetryService;
import com.moonju.preprocess.api.global.error.ErrorCode;
import com.moonju.preprocess.api.global.response.ApiResponse;
import com.moonju.preprocess.api.global.response.PageResponse;
import com.moonju.preprocess.api.global.support.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobCommandService jobCommandService;
    private final JobQueryService jobQueryService;
    private final JobCancelService jobCancelService;
    private final JobRetryService jobRetryService;

    public JobController(
        JobCommandService jobCommandService,
        JobQueryService jobQueryService,
        JobCancelService jobCancelService,
        JobRetryService jobRetryService
    ) {
        this.jobCommandService = jobCommandService;
        this.jobQueryService = jobQueryService;
        this.jobCancelService = jobCancelService;
        this.jobRetryService = jobRetryService;
    }

    @PostMapping
    public ApiResponse<JobCreateResponse> create(
        @CurrentUser Long currentUserId,
        @Valid @RequestBody JobCreateRequest request
    ) {
        return ApiResponse.success(ErrorCode.COMMON_CREATED, jobCommandService.create(currentUserId, request));
    }

    @GetMapping
    public ApiResponse<PageResponse<JobResponse>> findMyJobs(
        @CurrentUser Long currentUserId,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success(jobQueryService.findMyJobs(currentUserId, pageable));
    }

    @GetMapping("/{jobId}")
    public ApiResponse<JobResponse> findOne(@CurrentUser Long currentUserId, @PathVariable Long jobId) {
        return ApiResponse.success(jobQueryService.findOne(currentUserId, jobId));
    }

    @GetMapping("/{jobId}/items")
    public ApiResponse<PageResponse<JobItemResponse>> findItems(
        @CurrentUser Long currentUserId,
        @PathVariable Long jobId,
        @PageableDefault(size = 50) Pageable pageable
    ) {
        return ApiResponse.success(jobQueryService.findItems(currentUserId, jobId, pageable));
    }

    @GetMapping("/{jobId}/summary")
    public ApiResponse<JobSummaryResponse> summary(@CurrentUser Long currentUserId, @PathVariable Long jobId) {
        return ApiResponse.success(jobQueryService.summary(currentUserId, jobId));
    }

    @PostMapping("/{jobId}/cancel")
    public ApiResponse<JobCancelResponse> cancel(@CurrentUser Long currentUserId, @PathVariable Long jobId) {
        return ApiResponse.success(jobCancelService.cancel(currentUserId, jobId));
    }

    @PostMapping("/{jobId}/retry")
    public ApiResponse<JobRetryResponse> retry(@CurrentUser Long currentUserId, @PathVariable Long jobId) {
        return ApiResponse.success(jobRetryService.retryFailed(currentUserId, jobId));
    }

    @PostMapping("/{jobId}/rerun")
    public ApiResponse<JobRetryResponse> rerun(@CurrentUser Long currentUserId, @PathVariable Long jobId) {
        return ApiResponse.success(jobRetryService.rerun(currentUserId, jobId));
    }

    @GetMapping("/{jobId}/artifacts")
    public ApiResponse<JobArtifactResponse> artifacts(@CurrentUser Long currentUserId, @PathVariable Long jobId) {
        return ApiResponse.success(jobQueryService.artifacts(currentUserId, jobId));
    }

    @GetMapping("/{jobId}/download.zip")
    public ApiResponse<JobArtifactResponse> downloadZip(@CurrentUser Long currentUserId, @PathVariable Long jobId) {
        return ApiResponse.success(jobQueryService.artifacts(currentUserId, jobId));
    }

    @GetMapping("/{jobId}/items/{itemId}/download")
    public ApiResponse<JobItemDownloadUrlResponse> createItemDownloadUrl(
        @CurrentUser Long currentUserId,
        @PathVariable Long jobId,
        @PathVariable Long itemId,
        @RequestParam(defaultValue = "processed") String type
    ) {
        return ApiResponse.success(jobQueryService.createItemDownloadUrl(currentUserId, jobId, itemId, type));
    }
}
