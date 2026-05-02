package com.moonju.preprocess.api.domain.project.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProjectTests {

    @Test
    void createsActiveProject() {
        Project project = Project.create(1L, "Library scan", "A4 document batch", "LOW_CONTRAST_SCAN");

        assertThat(project.getOwnerId()).isEqualTo(1L);
        assertThat(project.getName()).isEqualTo("Library scan");
        assertThat(project.getDescription()).isEqualTo("A4 document batch");
        assertThat(project.getDefaultPreset()).isEqualTo("LOW_CONTRAST_SCAN");
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
    }

    @Test
    void updatesProjectMetadata() {
        Project project = Project.create(1L, "Library scan", "A4 document batch", "LOW_CONTRAST_SCAN");

        project.update("Receipt scan", "Receipt batch", "RECEIPT");

        assertThat(project.getName()).isEqualTo("Receipt scan");
        assertThat(project.getDescription()).isEqualTo("Receipt batch");
        assertThat(project.getDefaultPreset()).isEqualTo("RECEIPT");
    }

    @Test
    void softDeletesProject() {
        Project project = Project.create(1L, "Library scan", null, "LOW_CONTRAST_SCAN");

        project.delete();

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.DELETED);
        assertThat(project.isDeleted()).isTrue();
    }
}
