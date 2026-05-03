package com.moonju.preprocess.api.domain.auth.service;

import java.time.Instant;

public record IssuedTokenPair(
    String accessToken,
    Instant accessTokenExpiresAt,
    String refreshToken
) {
}
