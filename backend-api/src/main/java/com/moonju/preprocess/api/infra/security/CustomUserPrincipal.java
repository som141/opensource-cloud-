package com.moonju.preprocess.api.infra.security;

import com.moonju.preprocess.api.domain.user.entity.UserRole;

public record CustomUserPrincipal(
    Long userId,
    String email,
    UserRole role
) {
}
