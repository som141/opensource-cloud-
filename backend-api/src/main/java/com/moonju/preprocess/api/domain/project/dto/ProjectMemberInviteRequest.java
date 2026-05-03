package com.moonju.preprocess.api.domain.project.dto;

import com.moonju.preprocess.api.domain.project.entity.ProjectRole;
import jakarta.validation.constraints.NotNull;

public record ProjectMemberInviteRequest(
    @NotNull
    Long userId,

    @NotNull
    ProjectRole role
) {
}
