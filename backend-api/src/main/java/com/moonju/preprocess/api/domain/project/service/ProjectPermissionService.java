package com.moonju.preprocess.api.domain.project.service;

import com.moonju.preprocess.api.domain.project.entity.ProjectMember;
import com.moonju.preprocess.api.domain.project.entity.ProjectStatus;
import com.moonju.preprocess.api.domain.project.exception.ProjectAccessDeniedException;
import com.moonju.preprocess.api.domain.project.repository.ProjectMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectPermissionService {

    private final ProjectMemberRepository projectMemberRepository;

    public ProjectPermissionService(ProjectMemberRepository projectMemberRepository) {
        this.projectMemberRepository = projectMemberRepository;
    }

    @Transactional(readOnly = true)
    public ProjectMember validateReadable(Long projectId, Long userId) {
        ProjectMember member = findActiveMember(projectId, userId);
        if (!member.getRole().canRead()) {
            throw new ProjectAccessDeniedException();
        }
        return member;
    }

    @Transactional(readOnly = true)
    public ProjectMember validateEditable(Long projectId, Long userId) {
        ProjectMember member = findActiveMember(projectId, userId);
        if (!member.getRole().canEdit()) {
            throw new ProjectAccessDeniedException();
        }
        return member;
    }

    @Transactional(readOnly = true)
    public ProjectMember validateOwner(Long projectId, Long userId) {
        ProjectMember member = findActiveMember(projectId, userId);
        if (!member.getRole().canManageMembers()) {
            throw new ProjectAccessDeniedException();
        }
        return member;
    }

    private ProjectMember findActiveMember(Long projectId, Long userId) {
        return projectMemberRepository.findByProject_IdAndUser_IdAndProject_Status(
                projectId,
                userId,
                ProjectStatus.ACTIVE
            )
            .orElseThrow(ProjectAccessDeniedException::new);
    }
}
