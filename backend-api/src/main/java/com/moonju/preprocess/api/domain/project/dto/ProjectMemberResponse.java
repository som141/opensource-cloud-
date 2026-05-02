package com.moonju.preprocess.api.domain.project.dto;

import com.moonju.preprocess.api.domain.project.entity.ProjectMember;
import com.moonju.preprocess.api.domain.project.entity.ProjectRole;

public record ProjectMemberResponse(
    Long id,
    Long projectId,
    Long userId,
    ProjectRole role
) {

    public static ProjectMemberResponse from(ProjectMember member) {
        return new ProjectMemberResponse(
            member.getId(),
            member.getProjectId(),
            member.getUserId(),
            member.getRole()
        );
    }
}
