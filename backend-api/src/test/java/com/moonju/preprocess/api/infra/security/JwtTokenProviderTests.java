package com.moonju.preprocess.api.infra.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moonju.preprocess.api.domain.user.entity.User;
import com.moonju.preprocess.api.domain.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenProviderTests {

    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
        new ObjectMapper(),
        "test-secret-key-that-is-long-enough-for-hmac",
        1800L
    );

    @Test
    void createsAndParsesAccessToken() {
        User user = User.createUser("moonju@example.com", "MoonJu", null);
        ReflectionTestUtils.setField(user, "id", 1L);

        JwtTokenProvider.JwtAccessToken accessToken = jwtTokenProvider.createAccessToken(user);

        assertThat(accessToken.value()).isNotBlank();
        assertThat(accessToken.expiresAt()).isNotNull();
        assertThat(jwtTokenProvider.parseAccessToken(accessToken.value()))
            .hasValueSatisfying(authentication -> {
                assertThat(authentication.userId()).isEqualTo(1L);
                assertThat(authentication.email()).isEqualTo("moonju@example.com");
                assertThat(authentication.role()).isEqualTo(UserRole.USER);
            });
    }

    @Test
    void rejectsTamperedAccessToken() {
        User user = User.createUser("moonju@example.com", "MoonJu", null);
        ReflectionTestUtils.setField(user, "id", 1L);
        String accessToken = jwtTokenProvider.createAccessToken(user).value();

        String tampered = accessToken.substring(0, accessToken.length() - 2) + "xx";

        assertThat(jwtTokenProvider.parseAccessToken(tampered)).isEmpty();
    }
}
