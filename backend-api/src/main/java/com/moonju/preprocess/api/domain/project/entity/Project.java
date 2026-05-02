package com.moonju.preprocess.api.domain.project.entity;

import com.moonju.preprocess.api.global.support.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "projects")
public class Project extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, length = 60)
    private String defaultPreset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProjectStatus status;

    protected Project() {
    }

    public Project(Long ownerId, String name, String description, String defaultPreset, ProjectStatus status) {
        this.ownerId = ownerId;
        this.name = name;
        this.description = description;
        this.defaultPreset = defaultPreset;
        this.status = status;
    }

    public static Project create(Long ownerId, String name, String description, String defaultPreset) {
        return new Project(ownerId, name, description, defaultPreset, ProjectStatus.ACTIVE);
    }

    public void update(String name, String description, String defaultPreset) {
        this.name = name;
        this.description = description;
        this.defaultPreset = defaultPreset;
    }

    public void delete() {
        this.status = ProjectStatus.DELETED;
    }

    public boolean isDeleted() {
        return status == ProjectStatus.DELETED;
    }

    public Long getId() {
        return id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultPreset() {
        return defaultPreset;
    }

    public ProjectStatus getStatus() {
        return status;
    }
}
