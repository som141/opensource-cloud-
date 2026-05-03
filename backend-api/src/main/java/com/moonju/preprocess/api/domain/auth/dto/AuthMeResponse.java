package com.moonju.preprocess.api.domain.auth.dto;

import com.moonju.preprocess.api.domain.user.entity.SocialProvider;
import com.moonju.preprocess.api.domain.user.entity.User;
import com.moonju.preprocess.api.domain.user.entity.UserRole;
import java.util.List;

public record AuthMeResponse(
    Long id,
    String email,
    String name,
    String profileImageUrl,
    UserRole role,
    List<SocialProvider> providers
) {

    public static AuthMeResponse of(User user, List<SocialProvider> providers) {
        return new AuthMeResponse(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getProfileImageUrl(),
            user.getRole(),
            providers
        );
    }
}
