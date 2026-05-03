package com.moonju.preprocess.api.infra.oauth;

public interface OAuth2UserInfo {

    String getProviderUserId();

    String getEmail();

    String getName();

    String getProfileImageUrl();
}
