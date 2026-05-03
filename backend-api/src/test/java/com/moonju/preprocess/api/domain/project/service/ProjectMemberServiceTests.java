package com.moonju.preprocess.api.domain.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.api.domain.project.dto.ProjectMemberInviteRequest;
import com.moonju.preprocess.api.domain.project.dto.ProjectMemberResponse;
import com.moonju.preprocess.api.domain.project.entity.Project;
import com.moonju.preprocess.api.domain.project.entity.ProjectMember;
import com.moonju.preprocess.api.domain.project.entity.ProjectRole;
import com.moonju.preprocess.api.domain.project.exception.InvalidProjectMemberRoleException;
import com.moonju.preprocess.api.domain.project.repository.ProjectMemberRepository;
import com.moonju.preprocess.api.domain.user.entity.User;
import com.moonju.preprocess.api.domain.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceTests {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectPermissionService projectPermissionService;

    @Test
    void invitesEditorMember() {
        ProjectMemberService memberService = memberService();
        User owner = user(1L, "owner@example.com");
        User editor = user(2L, "editor@example.com");
        Project project = project(owner);
        when(projectPermissionService.validateOwner(10L, 1L))
            .thenReturn(new ProjectMember(project, owner, ProjectRole.OWNER));
        when(projectMemberRepository.existsByProject_IdAndUser_Id(10L, 2L)).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(editor));
        when(projectMemberRepository.save(any(ProjectMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectMemberResponse response = memberService.invite(
            1L,
            10L,
            new ProjectMemberInviteRequest(2L, ProjectRole.EDITOR)
        );

        assertThat(response.userId()).isEqualTo(2L);
        assertThat(response.role()).isEqualTo(ProjectRole.EDITOR);
    }

    @Test
    void rejectsOwnerInvite() {
        ProjectMemberService memberService = memberService();
        User owner = user(1L, "owner@example.com");
        when(projectPermissionService.validateOwner(10L, 1L))
            .thenReturn(new ProjectMember(project(owner), owner, ProjectRole.OWNER));

        assertThatThrownBy(() -> memberService.invite(
            1L,
            10L,
            new ProjectMemberInviteRequest(2L, ProjectRole.OWNER)
        )).isInstanceOf(InvalidProjectMemberRoleException.class);
    }

    @Test
    void removesNonOwnerMember() {
        ProjectMemberService memberService = memberService();
        User owner = user(1L, "owner@example.com");
        User viewer = user(2L, "viewer@example.com");
        Project project = project(owner);
        ProjectMember viewerMember = new ProjectMember(project, viewer, ProjectRole.VIEWER);
        when(projectMemberRepository.findByProject_IdAndUser_IdAndProject_Status(any(), any(), any()))
            .thenReturn(Optional.of(viewerMember));

        memberService.remove(1L, 10L, 2L);

        verify(projectPermissionService).validateOwner(10L, 1L);
        verify(projectMemberRepository).delete(viewerMember);
    }

    private ProjectMemberService memberService() {
        return new ProjectMemberService(projectMemberRepository, userRepository, projectPermissionService);
    }

    private Project project(User owner) {
        Project project = Project.create(owner, "Project", null, null);
        ReflectionTestUtils.setField(project, "id", 10L);
        return project;
    }

    private User user(Long id, String email) {
        User user = User.createUser(email, "User", null);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
