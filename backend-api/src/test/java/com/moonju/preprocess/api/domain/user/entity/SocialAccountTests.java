package com.moonju.preprocess.api.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SocialAccountTests {

    @Test
    void createsSocialAccount() {
        User user = User.createUser("moonju@example.com", "MoonJu", null);
        SocialAccount socialAccount = new SocialAccount(user, SocialProvider.GOOGLE, "google-123", user.getEmail());

        assertThat(socialAccount.getUser()).isSameAs(user);
        assertThat(socialAccount.getProvider()).isEqualTo(SocialProvider.GOOGLE);
        assertThat(socialAccount.getProviderUserId()).isEqualTo("google-123");
        assertThat(socialAccount.getEmail()).isEqualTo(user.getEmail());
    }
}
