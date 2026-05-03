package com.moonju.preprocess.api.domain.project.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.moonju.preprocess.api.domain.user.entity.User;
import org.junit.jupiter.api.Test;

class ProjectTests {

    @Test
    void createsActiveProject() {
        User owner = User.createUser("owner@example.com", "Owner", null);

        Project project = Project.create(owner, "Library Scan", "Old books", "LOW_CONTRAST_SCAN");

        assertThat(project.getOwner()).isSameAs(owner);
        assertThat(project.getName()).isEqualTo("Library Scan");
        assertThat(project.getDescription()).isEqualTo("Old books");
        assertThat(project.getDefaultPreset()).isEqualTo("LOW_CONTRAST_SCAN");
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
    }

    @Test
    void updatesProjectFields() {
        Project project = Project.create(User.createUser("owner@example.com", "Owner", null), "Old", null, null);

        project.update("New", "Description", "A4_SCAN_300DPI");

        assertThat(project.getName()).isEqualTo("New");
        assertThat(project.getDescription()).isEqualTo("Description");
        assertThat(project.getDefaultPreset()).isEqualTo("A4_SCAN_300DPI");
    }

    @Test
    void softDeletesProject() {
        Project project = Project.create(User.createUser("owner@example.com", "Owner", null), "Project", null, null);

        project.delete();

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.DELETED);
    }
}
