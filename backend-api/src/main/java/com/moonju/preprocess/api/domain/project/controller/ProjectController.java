package com.moonju.preprocess.api.domain.project.controller;

import com.moonju.preprocess.api.domain.project.dto.ProjectCreateRequest;
import com.moonju.preprocess.api.domain.project.dto.ProjectResponse;
import com.moonju.preprocess.api.domain.project.dto.ProjectSummaryResponse;
import com.moonju.preprocess.api.domain.project.dto.ProjectUpdateRequest;
import com.moonju.preprocess.api.domain.project.service.ProjectService;
import com.moonju.preprocess.api.global.error.ErrorCode;
import com.moonju.preprocess.api.global.response.ApiResponse;
import com.moonju.preprocess.api.global.response.PageResponse;
import com.moonju.preprocess.api.global.support.CurrentUser;
import com.moonju.preprocess.api.infra.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Projects", description = "Project CRUD, metadata, and summary APIs")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @Operation(summary = "Create project")
    public ApiResponse<ProjectResponse> create(
        @CurrentUser Long currentUserId,
        @Valid @RequestBody ProjectCreateRequest request
    ) {
        return ApiResponse.success(ErrorCode.COMMON_CREATED, projectService.create(currentUserId, request));
    }

    @GetMapping
    @Operation(summary = "List current user's projects")
    public ApiResponse<PageResponse<ProjectResponse>> findMyProjects(
        @CurrentUser Long currentUserId,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success(projectService.findMyProjects(currentUserId, pageable));
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "Read project detail")
    public ApiResponse<ProjectResponse> findOne(@CurrentUser Long currentUserId, @PathVariable Long projectId) {
        return ApiResponse.success(projectService.findOne(currentUserId, projectId));
    }

    @PatchMapping("/{projectId}")
    @Operation(summary = "Update project")
    public ApiResponse<ProjectResponse> update(
        @CurrentUser Long currentUserId,
        @PathVariable Long projectId,
        @Valid @RequestBody ProjectUpdateRequest request
    ) {
        return ApiResponse.success(projectService.update(currentUserId, projectId, request));
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "Soft delete project")
    public ApiResponse<Void> delete(@CurrentUser Long currentUserId, @PathVariable Long projectId) {
        projectService.delete(currentUserId, projectId);
        return ApiResponse.success(ErrorCode.COMMON_NO_CONTENT, null);
    }

    @GetMapping("/{projectId}/summary")
    @Operation(summary = "Read project summary")
    public ApiResponse<ProjectSummaryResponse> summary(@CurrentUser Long currentUserId, @PathVariable Long projectId) {
        return ApiResponse.success(projectService.summary(currentUserId, projectId));
    }
}
