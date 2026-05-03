package com.moonju.preprocess.api.domain.project.service;

import com.moonju.preprocess.api.domain.project.dto.ProjectMemberInviteRequest;
import com.moonju.preprocess.api.domain.project.dto.ProjectMemberResponse;
import com.moonju.preprocess.api.domain.project.entity.ProjectMember;
import com.moonju.preprocess.api.domain.project.entity.ProjectRole;
import com.moonju.preprocess.api.domain.project.entity.ProjectStatus;
import com.moonju.preprocess.api.domain.project.exception.InvalidProjectMemberRoleException;
import com.moonju.preprocess.api.domain.project.exception.ProjectMemberConflictException;
import com.moonju.preprocess.api.domain.project.exception.ProjectMemberNotFoundException;
import com.moonju.preprocess.api.domain.project.repository.ProjectMemberRepository;
import com.moonju.preprocess.api.domain.user.entity.User;
import com.moonju.preprocess.api.domain.user.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final ProjectPermissionService projectPermissionService;

    public ProjectMemberService(
        ProjectMemberRepository projectMemberRepository,
        UserRepository userRepository,
        ProjectPermissionService projectPermissionService
    ) {
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional
    public ProjectMemberResponse invite(Long currentUserId, Long projectId, ProjectMemberInviteRequest request) {
        ProjectMember ownerMember = projectPermissionService.validateOwner(projectId, currentUserId);
        validateInvitableRole(request.role());
        if (projectMemberRepository.existsByProject_IdAndUser_Id(projectId, request.userId())) {
            throw new ProjectMemberConflictException();
        }
        User invitedUser = userRepository.findById(request.userId())
            .orElseThrow(ProjectMemberNotFoundException::new);
        ProjectMember invitedMember = projectMemberRepository.save(new ProjectMember(
            ownerMember.getProject(),
            invitedUser,
            request.role()
        ));
        return ProjectMemberResponse.from(invitedMember);
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> findMembers(Long currentUserId, Long projectId) {
        projectPermissionService.validateReadable(projectId, currentUserId);
        return projectMemberRepository.findAllByProject_IdAndProject_StatusOrderByIdAsc(projectId, ProjectStatus.ACTIVE)
            .stream()
            .map(ProjectMemberResponse::from)
            .toList();
    }

    @Transactional
    public void remove(Long currentUserId, Long projectId, Long targetUserId) {
        projectPermissionService.validateOwner(projectId, currentUserId);
        ProjectMember targetMember = projectMemberRepository.findByProject_IdAndUser_IdAndProject_Status(
                projectId,
                targetUserId,
                ProjectStatus.ACTIVE
            )
            .orElseThrow(ProjectMemberNotFoundException::new);
        if (targetMember.getRole() == ProjectRole.OWNER) {
            throw new InvalidProjectMemberRoleException("Project owner cannot be removed.");
        }
        projectMemberRepository.delete(targetMember);
    }

    private void validateInvitableRole(ProjectRole role) {
        if (role == ProjectRole.OWNER) {
            throw new InvalidProjectMemberRoleException("OWNER role cannot be invited.");
        }
    }
}
