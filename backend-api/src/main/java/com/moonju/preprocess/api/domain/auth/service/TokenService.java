package com.moonju.preprocess.api.domain.auth.service;

import com.moonju.preprocess.api.domain.auth.entity.RefreshToken;
import com.moonju.preprocess.api.domain.auth.exception.InvalidTokenException;
import com.moonju.preprocess.api.domain.auth.repository.RefreshTokenRepository;
import com.moonju.preprocess.api.domain.user.entity.User;
import com.moonju.preprocess.api.domain.user.repository.UserRepository;
import com.moonju.preprocess.api.infra.security.JwtTokenProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private static final int REFRESH_TOKEN_BYTES = 48;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long refreshTokenExpireSeconds;

    public TokenService(
        RefreshTokenRepository refreshTokenRepository,
        UserRepository userRepository,
        JwtTokenProvider jwtTokenProvider,
        @Value("${app.jwt.refresh-token-expire-seconds}") long refreshTokenExpireSeconds
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenExpireSeconds = refreshTokenExpireSeconds;
    }

    public IssuedTokenPair issue(User user) {
        JwtTokenProvider.JwtAccessToken accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = createRefreshToken(user.getId());
        return new IssuedTokenPair(accessToken.value(), accessToken.expiresAt(), refreshToken);
    }

    public IssuedTokenPair refresh(String rawRefreshToken) {
        RefreshToken refreshToken = findActiveRefreshToken(rawRefreshToken);
        refreshToken.revoke(LocalDateTime.now());
        User user = userRepository.findById(refreshToken.getUserId())
            .orElseThrow(() -> new InvalidTokenException("Refresh token user not found."));
        return issue(user);
    }

    public void revoke(String rawRefreshToken) {
        String tokenHash = hash(rawRefreshToken);
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByTokenHash(tokenHash);
        refreshToken.ifPresent(token -> token.revoke(LocalDateTime.now()));
    }

    private String createRefreshToken(Long userId) {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String rawRefreshToken = HexFormat.of().formatHex(bytes);
        LocalDateTime expiresAt = LocalDateTime.ofInstant(
            Instant.now().plusSeconds(refreshTokenExpireSeconds),
            ZoneOffset.UTC
        );
        refreshTokenRepository.save(new RefreshToken(userId, hash(rawRefreshToken), expiresAt));
        return rawRefreshToken;
    }

    private RefreshToken findActiveRefreshToken(String rawRefreshToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
            .orElseThrow(() -> new InvalidTokenException("Refresh token is invalid."));
        if (refreshToken.isRevoked()) {
            throw new InvalidTokenException("Refresh token has been revoked.");
        }
        if (refreshToken.isExpired(LocalDateTime.now())) {
            throw new InvalidTokenException("Refresh token has expired.");
        }
        return refreshToken;
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed).toLowerCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is not available.", exception);
        }
    }
}
