package com.moonju.preprocess.api.domain.project.dto;

import com.moonju.preprocess.api.domain.project.entity.Project;
import com.moonju.preprocess.api.domain.project.entity.ProjectStatus;

public record ProjectResponse(
    Long id,
    Long ownerId,
    String name,
    String description,
    String defaultPreset,
    ProjectStatus status
) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
            project.getId(),
            project.getOwnerId(),
            project.getName(),
            project.getDescription(),
            project.getDefaultPreset(),
            project.getStatus()
        );
    }
}
