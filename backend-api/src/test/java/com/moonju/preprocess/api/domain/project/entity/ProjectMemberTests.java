package com.moonju.preprocess.api.domain.project.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProjectMemberTests {

    @Test
    void createsOwnerMember() {
        ProjectMember member = ProjectMember.owner(1L, 10L);

        assertThat(member.getProjectId()).isEqualTo(1L);
        assertThat(member.getUserId()).isEqualTo(10L);
        assertThat(member.getRole()).isEqualTo(ProjectRole.OWNER);
    }

    @Test
    void changesRole() {
        ProjectMember member = ProjectMember.invite(1L, 11L, ProjectRole.VIEWER);

        member.changeRole(ProjectRole.EDITOR);

        assertThat(member.getRole()).isEqualTo(ProjectRole.EDITOR);
    }
}
