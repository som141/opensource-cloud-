package com.moonju.preprocess.api.infra.oauth;

import com.moonju.preprocess.api.domain.user.entity.SocialProvider;

public interface OAuth2UserInfo {

    SocialProvider getProvider();

    String getProviderUserId();

    String getEmail();

    String getName();

    String getProfileImageUrl();
}
