package com.moonju.preprocess.api.domain.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class RefreshTokenTests {

    @Test
    void createsRefreshToken() {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(14);
        RefreshToken refreshToken = new RefreshToken(1L, "hash", expiresAt);

        assertThat(refreshToken.getUserId()).isEqualTo(1L);
        assertThat(refreshToken.getTokenHash()).isEqualTo("hash");
        assertThat(refreshToken.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(refreshToken.isRevoked()).isFalse();
    }

    @Test
    void revokesRefreshToken() {
        RefreshToken refreshToken = new RefreshToken(1L, "hash", LocalDateTime.now().plusDays(14));
        LocalDateTime revokedAt = LocalDateTime.now();

        refreshToken.revoke(revokedAt);

        assertThat(refreshToken.isRevoked()).isTrue();
        assertThat(refreshToken.getRevokedAt()).isEqualTo(revokedAt);
    }
}
