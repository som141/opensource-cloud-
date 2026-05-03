package com.moonju.preprocess.api.domain.auth.dto;

import java.time.Instant;

public record TokenRefreshResponse(
    String accessToken,
    Instant accessTokenExpiresAt,
    String refreshToken
) {

    public TokenRefreshResponse withoutRefreshToken() {
        return new TokenRefreshResponse(accessToken, accessTokenExpiresAt, null);
    }
}
