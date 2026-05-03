package com.moonju.preprocess.api.infra.oauth;

import com.moonju.preprocess.api.domain.auth.exception.OAuth2LoginFailedException;
import java.util.Locale;
import java.util.Map;

public final class OAuth2UserInfoFactory {

    private OAuth2UserInfoFactory() {
    }

    public static OAuth2UserInfo create(String registrationId, Map<String, Object> attributes) {
        String normalizedRegistrationId = registrationId.toLowerCase(Locale.ROOT);
        return switch (normalizedRegistrationId) {
            case "google" -> new GoogleOAuth2UserInfo(attributes);
            default -> throw new OAuth2LoginFailedException("Unsupported OAuth2 provider: " + registrationId);
        };
    }
}
