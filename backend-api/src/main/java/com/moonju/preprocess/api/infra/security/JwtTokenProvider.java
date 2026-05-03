package com.moonju.preprocess.api.infra.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moonju.preprocess.api.domain.user.entity.User;
import com.moonju.preprocess.api.domain.user.entity.UserRole;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final TypeReference<Map<String, Object>> CLAIMS_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long accessTokenExpireSeconds;

    public JwtTokenProvider(
        ObjectMapper objectMapper,
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.access-token-expire-seconds}") long accessTokenExpireSeconds
    ) {
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenExpireSeconds = accessTokenExpireSeconds;
    }

    public JwtAccessToken createAccessToken(User user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(accessTokenExpireSeconds);
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", String.valueOf(user.getId()));
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());
        claims.put("iat", issuedAt.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());
        return new JwtAccessToken(createToken(claims), expiresAt);
    }

    public Optional<JwtAuthentication> parseAccessToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3 || !isValidSignature(parts)) {
                return Optional.empty();
            }
            Map<String, Object> claims = objectMapper.readValue(base64UrlDecode(parts[1]), CLAIMS_TYPE);
            long expiresAt = Long.parseLong(String.valueOf(claims.get("exp")));
            if (Instant.now().getEpochSecond() >= expiresAt) {
                return Optional.empty();
            }
            return Optional.of(new JwtAuthentication(
                Long.valueOf(String.valueOf(claims.get("sub"))),
                String.valueOf(claims.get("email")),
                UserRole.valueOf(String.valueOf(claims.get("role")))
            ));
        } catch (RuntimeException | java.io.IOException exception) {
            return Optional.empty();
        }
    }

    private String createToken(Map<String, Object> claims) {
        try {
            String header = base64UrlEncode(objectMapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
            String payload = base64UrlEncode(objectMapper.writeValueAsBytes(claims));
            String signingInput = header + "." + payload;
            return signingInput + "." + sign(signingInput);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Failed to create JWT payload.", exception);
        }
    }

    private boolean isValidSignature(String[] parts) {
        String signingInput = parts[0] + "." + parts[1];
        String expectedSignature = sign(signingInput);
        return java.security.MessageDigest.isEqual(
            expectedSignature.getBytes(StandardCharsets.UTF_8),
            parts[2].getBytes(StandardCharsets.UTF_8)
        );
    }

    private String sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret, HMAC_SHA256));
            return base64UrlEncode(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to sign JWT.", exception);
        }
    }

    private String base64UrlEncode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private byte[] base64UrlDecode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    public record JwtAccessToken(
        String value,
        Instant expiresAt
    ) {
    }

    public record JwtAuthentication(
        Long userId,
        String email,
        UserRole role
    ) {
    }
}
