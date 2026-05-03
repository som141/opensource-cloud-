package com.moonju.preprocess.api.domain.project.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.api.domain.project.dto.ProjectCreateRequest;
import com.moonju.preprocess.api.domain.project.dto.ProjectResponse;
import com.moonju.preprocess.api.domain.project.entity.ProjectRole;
import com.moonju.preprocess.api.domain.project.entity.ProjectStatus;
import com.moonju.preprocess.api.domain.project.service.ProjectService;
import com.moonju.preprocess.api.global.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectControllerTests {

    @Mock
    private ProjectService projectService;

    @Test
    void createsProjectWithCommonResponse() {
        ProjectController controller = new ProjectController(projectService);
        ProjectCreateRequest request = new ProjectCreateRequest("Project", null, null);
        ProjectResponse serviceResponse = new ProjectResponse(
            10L,
            "Project",
            null,
            null,
            ProjectStatus.ACTIVE,
            1L,
            "owner@example.com",
            ProjectRole.OWNER,
            null,
            null
        );
        when(projectService.create(1L, request)).thenReturn(serviceResponse);

        ApiResponse<ProjectResponse> response = controller.create(1L, request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.code()).isEqualTo("common201");
        assertThat(response.result()).isSameAs(serviceResponse);
    }
}
