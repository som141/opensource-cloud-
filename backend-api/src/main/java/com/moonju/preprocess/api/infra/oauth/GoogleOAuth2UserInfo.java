package com.moonju.preprocess.api.infra.oauth;

import com.moonju.preprocess.api.domain.user.entity.SocialProvider;
import java.util.Map;

public class GoogleOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public SocialProvider getProvider() {
        return SocialProvider.GOOGLE;
    }

    @Override
    public String getProviderUserId() {
        return asString("sub");
    }

    @Override
    public String getEmail() {
        return asString("email");
    }

    @Override
    public String getName() {
        return asString("name");
    }

    @Override
    public String getProfileImageUrl() {
        return asString("picture");
    }

    private String asString(String key) {
        Object value = attributes.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
