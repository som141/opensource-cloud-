package com.moonju.preprocess.api.domain.project.service;

import com.moonju.preprocess.api.domain.image.entity.ImageStatus;
import com.moonju.preprocess.api.domain.image.repository.ImageRepository;
import com.moonju.preprocess.api.domain.job.repository.JobRepository;
import com.moonju.preprocess.api.domain.project.dto.ProjectCreateRequest;
import com.moonju.preprocess.api.domain.project.dto.ProjectResponse;
import com.moonju.preprocess.api.domain.project.dto.ProjectSummaryResponse;
import com.moonju.preprocess.api.domain.project.dto.ProjectUpdateRequest;
import com.moonju.preprocess.api.domain.project.entity.Project;
import com.moonju.preprocess.api.domain.project.entity.ProjectMember;
import com.moonju.preprocess.api.domain.project.entity.ProjectRole;
import com.moonju.preprocess.api.domain.project.entity.ProjectStatus;
import com.moonju.preprocess.api.domain.project.exception.InvalidProjectMemberRoleException;
import com.moonju.preprocess.api.domain.project.repository.ProjectMemberRepository;
import com.moonju.preprocess.api.domain.project.repository.ProjectRepository;
import com.moonju.preprocess.api.domain.user.entity.User;
import com.moonju.preprocess.api.domain.user.repository.UserRepository;
import com.moonju.preprocess.api.global.response.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final ProjectPermissionService projectPermissionService;
    private final ImageRepository imageRepository;
    private final JobRepository jobRepository;

    public ProjectService(
        ProjectRepository projectRepository,
        ProjectMemberRepository projectMemberRepository,
        UserRepository userRepository,
        ProjectPermissionService projectPermissionService,
        ImageRepository imageRepository,
        JobRepository jobRepository
    ) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
        this.projectPermissionService = projectPermissionService;
        this.imageRepository = imageRepository;
        this.jobRepository = jobRepository;
    }

    @Transactional
    public ProjectResponse create(Long currentUserId, ProjectCreateRequest request) {
        User owner = findUser(currentUserId);
        Project project = projectRepository.save(Project.create(
            owner,
            request.name().trim(),
            request.description(),
            request.defaultPreset()
        ));
        projectMemberRepository.save(new ProjectMember(project, owner, ProjectRole.OWNER));
        return ProjectResponse.from(project, ProjectRole.OWNER);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> findMyProjects(Long currentUserId, Pageable pageable) {
        return PageResponse.from(projectMemberRepository
            .findAllByUser_IdAndProject_Status(currentUserId, ProjectStatus.ACTIVE, pageable)
            .map(member -> ProjectResponse.from(member.getProject(), member.getRole())));
    }

    @Transactional(readOnly = true)
    public ProjectResponse findOne(Long currentUserId, Long projectId) {
        ProjectMember member = projectPermissionService.validateReadable(projectId, currentUserId);
        return ProjectResponse.from(member.getProject(), member.getRole());
    }

    @Transactional
    public ProjectResponse update(Long currentUserId, Long projectId, ProjectUpdateRequest request) {
        ProjectMember member = projectPermissionService.validateEditable(projectId, currentUserId);
        member.getProject().update(
            normalizeName(request.name()),
            request.description(),
            request.defaultPreset()
        );
        return ProjectResponse.from(member.getProject(), member.getRole());
    }

    @Transactional
    public void delete(Long currentUserId, Long projectId) {
        ProjectMember member = projectPermissionService.validateOwner(projectId, currentUserId);
        member.getProject().delete();
    }

    @Transactional(readOnly = true)
    public ProjectSummaryResponse summary(Long currentUserId, Long projectId) {
        ProjectMember member = projectPermissionService.validateReadable(projectId, currentUserId);
        long memberCount = projectMemberRepository.countByProject_IdAndProject_Status(projectId, ProjectStatus.ACTIVE);
        long imageCount = imageRepository.countByProjectIdAndStatusNot(projectId, ImageStatus.DELETED);
        long jobCount = jobRepository.countByProjectId(projectId);
        return new ProjectSummaryResponse(
            member.getProject().getId(),
            member.getProject().getName(),
            memberCount,
            imageCount,
            jobCount
        );
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new InvalidProjectMemberRoleException("Project owner user not found."));
    }

    private String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        if (!StringUtils.hasText(name)) {
            throw new InvalidProjectMemberRoleException("Project name must not be blank.");
        }
        return name.trim();
    }
}
