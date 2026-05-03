package com.moonju.preprocess.api.domain.project.service;

import com.moonju.preprocess.api.domain.project.dto.ProjectMemberInviteRequest;
import com.moonju.preprocess.api.domain.project.dto.ProjectMemberResponse;
import com.moonju.preprocess.api.domain.project.entity.ProjectMember;
import com.moonju.preprocess.api.domain.project.repository.ProjectMemberRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectPermissionService projectPermissionService;

    public ProjectMemberService(
        ProjectMemberRepository projectMemberRepository,
        ProjectPermissionService projectPermissionService
    ) {
        this.projectMemberRepository = projectMemberRepository;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional
    public ProjectMemberResponse invite(Long currentUserId, Long projectId, ProjectMemberInviteRequest request) {
        projectPermissionService.validateOwner(currentUserId, projectId);
        ProjectMember member = ProjectMember.invite(projectId, request.userId(), request.role());
        return ProjectMemberResponse.from(projectMemberRepository.save(member));
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> findMembers(Long currentUserId, Long projectId) {
        projectPermissionService.validateReadable(currentUserId, projectId);
        return projectMemberRepository.findByProjectId(projectId)
            .stream()
            .map(ProjectMemberResponse::from)
            .toList();
    }

    @Transactional
    public void remove(Long currentUserId, Long projectId, Long userId) {
        projectPermissionService.validateOwner(currentUserId, projectId);
        projectMemberRepository.deleteByProjectIdAndUserId(projectId, userId);
    }
}
