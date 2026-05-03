package com.moonju.preprocess.api.domain.project.dto;

import com.moonju.preprocess.api.domain.project.entity.ProjectMember;
import com.moonju.preprocess.api.domain.project.entity.ProjectRole;
import java.time.LocalDateTime;

public record ProjectMemberResponse(
    Long id,
    Long userId,
    String email,
    String name,
    ProjectRole role,
    LocalDateTime createdAt
) {

    public static ProjectMemberResponse from(ProjectMember member) {
        return new ProjectMemberResponse(
            member.getId(),
            member.getUser().getId(),
            member.getUser().getEmail(),
            member.getUser().getName(),
            member.getRole(),
            member.getCreatedAt()
        );
    }
}
