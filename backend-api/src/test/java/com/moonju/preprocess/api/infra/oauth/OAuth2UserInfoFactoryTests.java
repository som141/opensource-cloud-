package com.moonju.preprocess.api.infra.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.moonju.preprocess.api.domain.auth.exception.OAuth2LoginFailedException;
import com.moonju.preprocess.api.domain.user.entity.SocialProvider;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OAuth2UserInfoFactoryTests {

    @Test
    void createsGoogleUserInfo() {
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.create(
            "google",
            Map.of(
                "sub", "google-123",
                "email", "moonju@example.com",
                "name", "MoonJu",
                "picture", "https://example.com/profile.png"
            )
        );

        assertThat(userInfo.getProvider()).isEqualTo(SocialProvider.GOOGLE);
        assertThat(userInfo.getProviderUserId()).isEqualTo("google-123");
        assertThat(userInfo.getEmail()).isEqualTo("moonju@example.com");
        assertThat(userInfo.getName()).isEqualTo("MoonJu");
        assertThat(userInfo.getProfileImageUrl()).isEqualTo("https://example.com/profile.png");
    }

    @Test
    void createsKakaoUserInfo() {
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.create(
            "kakao",
            Map.of(
                "id", 12345L,
                "kakao_account", Map.of(
                    "email", "moonju@example.com",
                    "profile", Map.of(
                        "nickname", "MoonJu",
                        "profile_image_url", "https://example.com/kakao.png"
                    )
                )
            )
        );

        assertThat(userInfo.getProvider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(userInfo.getProviderUserId()).isEqualTo("12345");
        assertThat(userInfo.getEmail()).isEqualTo("moonju@example.com");
        assertThat(userInfo.getName()).isEqualTo("MoonJu");
        assertThat(userInfo.getProfileImageUrl()).isEqualTo("https://example.com/kakao.png");
    }

    @Test
    void rejectsUnsupportedProvider() {
        assertThatThrownBy(() -> OAuth2UserInfoFactory.create("github", Map.of()))
            .isInstanceOf(OAuth2LoginFailedException.class)
            .hasMessageContaining("Unsupported OAuth2 provider");
    }
}
