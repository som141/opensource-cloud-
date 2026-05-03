package com.moonju.preprocess.api.domain.project.controller;

import com.moonju.preprocess.api.domain.project.dto.ProjectMemberInviteRequest;
import com.moonju.preprocess.api.domain.project.dto.ProjectMemberResponse;
import com.moonju.preprocess.api.domain.project.service.ProjectMemberService;
import com.moonju.preprocess.api.global.error.ErrorCode;
import com.moonju.preprocess.api.global.response.ApiResponse;
import com.moonju.preprocess.api.global.support.CurrentUser;
import com.moonju.preprocess.api.infra.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/members")
@Tag(name = "Project Members", description = "Project member invitation, listing, and removal APIs")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    public ProjectMemberController(ProjectMemberService projectMemberService) {
        this.projectMemberService = projectMemberService;
    }

    @PostMapping
    @Operation(summary = "Invite project member")
    public ApiResponse<ProjectMemberResponse> invite(
        @CurrentUser Long currentUserId,
        @PathVariable Long projectId,
        @Valid @RequestBody ProjectMemberInviteRequest request
    ) {
        return ApiResponse.success(ErrorCode.COMMON_CREATED, projectMemberService.invite(
            currentUserId,
            projectId,
            request
        ));
    }

    @GetMapping
    @Operation(summary = "List project members")
    public ApiResponse<List<ProjectMemberResponse>> findMembers(
        @CurrentUser Long currentUserId,
        @PathVariable Long projectId
    ) {
        return ApiResponse.success(projectMemberService.findMembers(currentUserId, projectId));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Remove project member")
    public ApiResponse<Void> remove(
        @CurrentUser Long currentUserId,
        @PathVariable Long projectId,
        @PathVariable Long userId
    ) {
        projectMemberService.remove(currentUserId, projectId, userId);
        return ApiResponse.success(ErrorCode.COMMON_NO_CONTENT, null);
    }
}
