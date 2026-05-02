package com.moonju.preprocess.api.domain.project.service;

import com.moonju.preprocess.api.domain.project.dto.ProjectCreateRequest;
import com.moonju.preprocess.api.domain.project.dto.ProjectResponse;
import com.moonju.preprocess.api.domain.project.dto.ProjectSummaryResponse;
import com.moonju.preprocess.api.domain.project.dto.ProjectUpdateRequest;
import com.moonju.preprocess.api.domain.project.entity.Project;
import com.moonju.preprocess.api.domain.project.entity.ProjectMember;
import com.moonju.preprocess.api.domain.project.entity.ProjectStatus;
import com.moonju.preprocess.api.domain.project.exception.ProjectNotFoundException;
import com.moonju.preprocess.api.domain.project.repository.ProjectMemberRepository;
import com.moonju.preprocess.api.domain.project.repository.ProjectRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectPermissionService projectPermissionService;

    public ProjectService(
        ProjectRepository projectRepository,
        ProjectMemberRepository projectMemberRepository,
        ProjectPermissionService projectPermissionService
    ) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional
    public ProjectResponse create(Long currentUserId, ProjectCreateRequest request) {
        Project project = projectRepository.save(
            Project.create(currentUserId, request.name(), request.description(), request.defaultPreset())
        );
        projectMemberRepository.save(ProjectMember.owner(project.getId(), currentUserId));
        return ProjectResponse.from(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> findMine(Long currentUserId) {
        return projectRepository.findByOwnerIdAndStatus(currentUserId, ProjectStatus.ACTIVE)
            .stream()
            .map(ProjectResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse findOne(Long currentUserId, Long projectId) {
        projectPermissionService.validateReadable(currentUserId, projectId);
        return ProjectResponse.from(findActiveProject(projectId));
    }

    @Transactional
    public ProjectResponse update(Long currentUserId, Long projectId, ProjectUpdateRequest request) {
        projectPermissionService.validateEditable(currentUserId, projectId);
        Project project = findActiveProject(projectId);
        project.update(request.name(), request.description(), request.defaultPreset());
        return ProjectResponse.from(project);
    }

    @Transactional
    public void delete(Long currentUserId, Long projectId) {
        projectPermissionService.validateOwner(currentUserId, projectId);
        Project project = findActiveProject(projectId);
        project.delete();
    }

    @Transactional(readOnly = true)
    public ProjectSummaryResponse summary(Long currentUserId, Long projectId) {
        projectPermissionService.validateReadable(currentUserId, projectId);
        findActiveProject(projectId);
        return ProjectSummaryResponse.empty(projectId);
    }

    private Project findActiveProject(Long projectId) {
        return projectRepository.findByIdAndStatus(projectId, ProjectStatus.ACTIVE)
            .orElseThrow(ProjectNotFoundException::new);
    }
}
