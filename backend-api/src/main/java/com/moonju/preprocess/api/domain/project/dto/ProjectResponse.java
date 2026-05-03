package com.moonju.preprocess.api.domain.project.dto;

import com.moonju.preprocess.api.domain.project.entity.Project;
import com.moonju.preprocess.api.domain.project.entity.ProjectRole;
import com.moonju.preprocess.api.domain.project.entity.ProjectStatus;
import java.time.LocalDateTime;

public record ProjectResponse(
    Long id,
    String name,
    String description,
    String defaultPreset,
    ProjectStatus status,
    Long ownerId,
    String ownerEmail,
    ProjectRole myRole,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static ProjectResponse from(Project project, ProjectRole myRole) {
        return new ProjectResponse(
            project.getId(),
            project.getName(),
            project.getDescription(),
            project.getDefaultPreset(),
            project.getStatus(),
            project.getOwner().getId(),
            project.getOwner().getEmail(),
            myRole,
            project.getCreatedAt(),
            project.getUpdatedAt()
        );
    }
}
