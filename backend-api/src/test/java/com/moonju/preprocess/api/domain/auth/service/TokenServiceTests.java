package com.moonju.preprocess.api.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.api.domain.auth.entity.RefreshToken;
import com.moonju.preprocess.api.domain.auth.repository.RefreshTokenRepository;
import com.moonju.preprocess.api.domain.user.entity.User;
import com.moonju.preprocess.api.domain.user.repository.UserRepository;
import com.moonju.preprocess.api.infra.security.JwtTokenProvider;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenServiceTests {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void issuesAccessTokenAndRefreshToken() {
        TokenService tokenService = new TokenService(
            refreshTokenRepository,
            userRepository,
            jwtTokenProvider,
            1209600L
        );
        User user = User.createUser("moonju@example.com", "MoonJu", null);
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 1L);
        when(jwtTokenProvider.createAccessToken(user))
            .thenReturn(new JwtTokenProvider.JwtAccessToken("access-token", Instant.parse("2026-05-03T09:00:00Z")));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IssuedTokenPair tokenPair = tokenService.issue(user);

        assertThat(tokenPair.accessToken()).isEqualTo("access-token");
        assertThat(tokenPair.accessTokenExpiresAt()).isEqualTo(Instant.parse("2026-05-03T09:00:00Z"));
        assertThat(tokenPair.refreshToken()).isNotBlank();
    }

    @Test
    void refreshesTokenAndRevokesPreviousRefreshToken() {
        TokenService tokenService = new TokenService(
            refreshTokenRepository,
            userRepository,
            jwtTokenProvider,
            1209600L
        );
        User user = User.createUser("moonju@example.com", "MoonJu", null);
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 1L);
        RefreshToken previousRefreshToken = new RefreshToken(1L, "hash", LocalDateTime.now().plusDays(1));

        when(refreshTokenRepository.findByTokenHash(any(String.class))).thenReturn(Optional.of(previousRefreshToken));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.createAccessToken(user))
            .thenReturn(new JwtTokenProvider.JwtAccessToken("new-access-token", Instant.parse("2026-05-03T09:00:00Z")));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IssuedTokenPair tokenPair = tokenService.refresh("previous-refresh-token");

        assertThat(previousRefreshToken.isRevoked()).isTrue();
        assertThat(tokenPair.accessToken()).isEqualTo("new-access-token");
        assertThat(tokenPair.refreshToken()).isNotBlank();
    }
}
