package com.moonju.preprocess.api.domain.project.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.api.domain.project.dto.ProjectMemberInviteRequest;
import com.moonju.preprocess.api.domain.project.dto.ProjectMemberResponse;
import com.moonju.preprocess.api.domain.project.entity.ProjectRole;
import com.moonju.preprocess.api.domain.project.service.ProjectMemberService;
import com.moonju.preprocess.api.global.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectMemberControllerTests {

    @Mock
    private ProjectMemberService projectMemberService;

    @Test
    void invitesProjectMemberWithCommonResponse() {
        ProjectMemberController controller = new ProjectMemberController(projectMemberService);
        ProjectMemberInviteRequest request = new ProjectMemberInviteRequest(2L, ProjectRole.EDITOR);
        ProjectMemberResponse serviceResponse = new ProjectMemberResponse(
            100L,
            2L,
            "editor@example.com",
            "Editor",
            ProjectRole.EDITOR,
            null
        );
        when(projectMemberService.invite(1L, 10L, request)).thenReturn(serviceResponse);

        ApiResponse<ProjectMemberResponse> response = controller.invite(1L, 10L, request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.code()).isEqualTo("common201");
        assertThat(response.result()).isSameAs(serviceResponse);
    }
}
