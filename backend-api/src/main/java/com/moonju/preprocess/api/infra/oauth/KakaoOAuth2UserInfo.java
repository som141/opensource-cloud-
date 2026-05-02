package com.moonju.preprocess.api.infra.oauth;

import com.moonju.preprocess.api.domain.user.entity.SocialProvider;
import java.util.Collections;
import java.util.Map;

public class KakaoOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public SocialProvider getProvider() {
        return SocialProvider.KAKAO;
    }

    @Override
    public String getProviderUserId() {
        Object value = attributes.get("id");
        return value == null ? null : String.valueOf(value);
    }

    @Override
    public String getEmail() {
        Object value = kakaoAccount().get("email");
        return value == null ? null : String.valueOf(value);
    }

    @Override
    public String getName() {
        Object value = profile().get("nickname");
        return value == null ? null : String.valueOf(value);
    }

    @Override
    public String getProfileImageUrl() {
        Object value = profile().get("profile_image_url");
        return value == null ? null : String.valueOf(value);
    }

    private Map<String, Object> kakaoAccount() {
        return asMap(attributes.get("kakao_account"));
    }

    private Map<String, Object> profile() {
        return asMap(kakaoAccount().get("profile"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Collections.emptyMap();
    }
}
