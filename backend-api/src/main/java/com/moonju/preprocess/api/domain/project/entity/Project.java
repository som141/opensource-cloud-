package com.moonju.preprocess.api.domain.project.entity;

import com.moonju.preprocess.api.domain.user.entity.User;
import com.moonju.preprocess.api.global.support.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "projects")
public class Project extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(length = 100)
    private String defaultPreset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProjectStatus status;

    protected Project() {
    }

    private Project(User owner, String name, String description, String defaultPreset) {
        this.owner = owner;
        this.name = name;
        this.description = description;
        this.defaultPreset = defaultPreset;
        this.status = ProjectStatus.ACTIVE;
    }

    public static Project create(User owner, String name, String description, String defaultPreset) {
        return new Project(owner, name, description, defaultPreset);
    }

    public void update(String name, String description, String defaultPreset) {
        if (name != null) {
            this.name = name;
        }
        if (description != null) {
            this.description = description;
        }
        if (defaultPreset != null) {
            this.defaultPreset = defaultPreset;
        }
    }

    public void delete() {
        this.status = ProjectStatus.DELETED;
    }

    public Long getId() {
        return id;
    }

    public User getOwner() {
        return owner;
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
