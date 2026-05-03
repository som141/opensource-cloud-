package com.moonju.preprocess.api.infra.oauth;

import java.util.Map;

public class GoogleOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProviderUserId() {
        return String.valueOf(attributes.get("sub"));
    }

    @Override
    public String getEmail() {
        return String.valueOf(attributes.get("email"));
    }

    @Override
    public String getName() {
        return String.valueOf(attributes.getOrDefault("name", getEmail()));
    }

    @Override
    public String getProfileImageUrl() {
        Object picture = attributes.get("picture");
        return picture == null ? null : String.valueOf(picture);
    }
}
