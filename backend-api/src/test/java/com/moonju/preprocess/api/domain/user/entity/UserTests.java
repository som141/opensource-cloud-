package com.moonju.preprocess.api.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserTests {

    @Test
    void createsDefaultUser() {
        User user = User.createUser("moonju@example.com", "MoonJu", "https://example.com/profile.png");

        assertThat(user.getEmail()).isEqualTo("moonju@example.com");
        assertThat(user.getName()).isEqualTo("MoonJu");
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void marksUserDeleted() {
        User user = User.createUser("moonju@example.com", "MoonJu", null);

        user.delete();

        assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
    }
}
