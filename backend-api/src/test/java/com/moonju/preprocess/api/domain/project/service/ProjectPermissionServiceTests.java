package com.moonju.preprocess.api.domain.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.api.domain.project.entity.Project;
import com.moonju.preprocess.api.domain.project.entity.ProjectMember;
import com.moonju.preprocess.api.domain.project.entity.ProjectRole;
import com.moonju.preprocess.api.domain.project.entity.ProjectStatus;
import com.moonju.preprocess.api.domain.project.exception.ProjectAccessDeniedException;
import com.moonju.preprocess.api.domain.project.repository.ProjectMemberRepository;
import com.moonju.preprocess.api.domain.user.entity.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectPermissionServiceTests {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Test
    void validatesEditorEditablePermission() {
        ProjectPermissionService permissionService = new ProjectPermissionService(projectMemberRepository);
        ProjectMember member = member(ProjectRole.EDITOR);
        when(projectMemberRepository.findByProject_IdAndUser_IdAndProject_Status(1L, 2L, ProjectStatus.ACTIVE))
            .thenReturn(Optional.of(member));

        ProjectMember result = permissionService.validateEditable(1L, 2L);

        assertThat(result).isSameAs(member);
    }

    @Test
    void rejectsViewerEditablePermission() {
        ProjectPermissionService permissionService = new ProjectPermissionService(projectMemberRepository);
        when(projectMemberRepository.findByProject_IdAndUser_IdAndProject_Status(1L, 2L, ProjectStatus.ACTIVE))
            .thenReturn(Optional.of(member(ProjectRole.VIEWER)));

        assertThatThrownBy(() -> permissionService.validateEditable(1L, 2L))
            .isInstanceOf(ProjectAccessDeniedException.class);
    }

    @Test
    void rejectsMissingMember() {
        ProjectPermissionService permissionService = new ProjectPermissionService(projectMemberRepository);
        when(projectMemberRepository.findByProject_IdAndUser_IdAndProject_Status(1L, 2L, ProjectStatus.ACTIVE))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> permissionService.validateReadable(1L, 2L))
            .isInstanceOf(ProjectAccessDeniedException.class);
    }

    private ProjectMember member(ProjectRole role) {
        User user = User.createUser("user@example.com", "User", null);
        Project project = Project.create(user, "Project", null, null);
        return new ProjectMember(project, user, role);
    }
}
