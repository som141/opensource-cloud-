package com.moonju.preprocess.api.domain.project.service;

import com.moonju.preprocess.api.domain.project.entity.ProjectRole;
import com.moonju.preprocess.api.domain.project.exception.ProjectAccessDeniedException;
import com.moonju.preprocess.api.domain.project.repository.ProjectMemberRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectPermissionService {

    private static final List<ProjectRole> READABLE_ROLES = List.of(
        ProjectRole.OWNER,
        ProjectRole.EDITOR,
        ProjectRole.VIEWER
    );
    private static final List<ProjectRole> EDITABLE_ROLES = List.of(ProjectRole.OWNER, ProjectRole.EDITOR);
    private static final List<ProjectRole> OWNER_ROLES = List.of(ProjectRole.OWNER);

    private final ProjectMemberRepository projectMemberRepository;

    public ProjectPermissionService(ProjectMemberRepository projectMemberRepository) {
        this.projectMemberRepository = projectMemberRepository;
    }

    @Transactional(readOnly = true)
    public void validateReadable(Long userId, Long projectId) {
        validateRole(userId, projectId, READABLE_ROLES);
    }

    @Transactional(readOnly = true)
    public void validateEditable(Long userId, Long projectId) {
        validateRole(userId, projectId, EDITABLE_ROLES);
    }

    @Transactional(readOnly = true)
    public void validateOwner(Long userId, Long projectId) {
        validateRole(userId, projectId, OWNER_ROLES);
    }

    private void validateRole(Long userId, Long projectId, List<ProjectRole> allowedRoles) {
        boolean allowed = projectMemberRepository.existsByProjectIdAndUserIdAndRoleIn(projectId, userId, allowedRoles);
        if (!allowed) {
            throw new ProjectAccessDeniedException();
        }
    }
}
