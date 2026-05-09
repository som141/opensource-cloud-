package com.moonju.preprocess.api.domain.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.api.domain.image.entity.ImageStatus;
import com.moonju.preprocess.api.domain.image.repository.ImageRepository;
import com.moonju.preprocess.api.domain.project.dto.ProjectCreateRequest;
import com.moonju.preprocess.api.domain.project.dto.ProjectResponse;
import com.moonju.preprocess.api.domain.project.dto.ProjectSummaryResponse;
import com.moonju.preprocess.api.domain.project.dto.ProjectUpdateRequest;
import com.moonju.preprocess.api.domain.project.entity.Project;
import com.moonju.preprocess.api.domain.project.entity.ProjectMember;
import com.moonju.preprocess.api.domain.project.entity.ProjectRole;
import com.moonju.preprocess.api.domain.project.entity.ProjectStatus;
import com.moonju.preprocess.api.domain.project.repository.ProjectMemberRepository;
import com.moonju.preprocess.api.domain.project.repository.ProjectRepository;
import com.moonju.preprocess.api.domain.user.entity.User;
import com.moonju.preprocess.api.domain.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTests {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectPermissionService projectPermissionService;

    @Mock
    private ImageRepository imageRepository;

    @Test
    void createsProjectAndOwnerMember() {
        ProjectService projectService = projectService();
        User owner = user(1L, "owner@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            ReflectionTestUtils.setField(project, "id", 10L);
            return project;
        });

        ProjectResponse response = projectService.create(
            1L,
            new ProjectCreateRequest(" Project ", "Description", "LOW_CONTRAST_SCAN")
        );

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("Project");
        assertThat(response.myRole()).isEqualTo(ProjectRole.OWNER);
        verify(projectMemberRepository).save(any(ProjectMember.class));
    }

    @Test
    void updatesProjectWhenUserCanEdit() {
        ProjectService projectService = projectService();
        User owner = user(1L, "owner@example.com");
        Project project = Project.create(owner, "Old", null, null);
        ReflectionTestUtils.setField(project, "id", 10L);
        when(projectPermissionService.validateEditable(10L, 1L))
            .thenReturn(new ProjectMember(project, owner, ProjectRole.EDITOR));

        ProjectResponse response = projectService.update(
            1L,
            10L,
            new ProjectUpdateRequest("New", "Next", "A4_SCAN_300DPI")
        );

        assertThat(response.name()).isEqualTo("New");
        assertThat(response.description()).isEqualTo("Next");
        assertThat(response.defaultPreset()).isEqualTo("A4_SCAN_300DPI");
        assertThat(response.myRole()).isEqualTo(ProjectRole.EDITOR);
    }

    @Test
    void summarizesProjectWithImageCount() {
        ProjectService projectService = projectService();
        User owner = user(1L, "owner@example.com");
        Project project = Project.create(owner, "Project", null, null);
        ReflectionTestUtils.setField(project, "id", 10L);
        when(projectPermissionService.validateReadable(10L, 1L))
            .thenReturn(new ProjectMember(project, owner, ProjectRole.OWNER));
        when(projectMemberRepository.countByProject_IdAndProject_Status(10L, ProjectStatus.ACTIVE)).thenReturn(2L);
        when(imageRepository.countByProjectIdAndStatusNot(10L, ImageStatus.DELETED)).thenReturn(5L);

        ProjectSummaryResponse response = projectService.summary(1L, 10L);

        assertThat(response.memberCount()).isEqualTo(2L);
        assertThat(response.imageCount()).isEqualTo(5L);
        assertThat(response.jobCount()).isZero();
    }

    private ProjectService projectService() {
        return new ProjectService(
            projectRepository,
            projectMemberRepository,
            userRepository,
            projectPermissionService,
            imageRepository
        );
    }

    private User user(Long id, String email) {
        User user = User.createUser(email, "User", null);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
