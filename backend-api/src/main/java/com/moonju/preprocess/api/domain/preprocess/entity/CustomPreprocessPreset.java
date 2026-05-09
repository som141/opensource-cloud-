package com.moonju.preprocess.api.domain.preprocess.entity;

import com.moonju.preprocess.api.global.support.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "custom_preprocess_presets")
public class CustomPreprocessPreset extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PreprocessPresetName basePresetName;

    @ElementCollection
    @CollectionTable(
        name = "custom_preprocess_preset_parameters",
        joinColumns = @JoinColumn(name = "custom_preset_id")
    )
    @MapKeyColumn(name = "parameter_name", length = 100)
    @Column(name = "parameter_value", length = 500)
    private Map<String, String> parameters = new LinkedHashMap<>();

    @Column(nullable = false)
    private boolean deleted;

    private LocalDateTime deletedAt;

    protected CustomPreprocessPreset() {
    }

    public CustomPreprocessPreset(
        Long userId,
        String name,
        String description,
        PreprocessPresetName basePresetName,
        Map<String, String> parameters
    ) {
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.basePresetName = basePresetName;
        this.parameters = new LinkedHashMap<>(parameters);
        this.deleted = false;
    }

    public void delete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public PreprocessPresetName getBasePresetName() {
        return basePresetName;
    }

    public Map<String, String> getParameters() {
        return Map.copyOf(parameters);
    }

    public boolean isDeleted() {
        return deleted;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
