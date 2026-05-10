package com.moonju.preprocess.api.domain.job.entity;

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
@Table(name = "jobs")
public class Job extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String preset;

    @ElementCollection
    @CollectionTable(name = "job_preset_parameters", joinColumns = @JoinColumn(name = "job_id"))
    @MapKeyColumn(name = "parameter_name", length = 100)
    @Column(name = "parameter_value", length = 500)
    private Map<String, String> presetParameters = new LinkedHashMap<>();

    @Column(nullable = false)
    private boolean debug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private JobStatus status;

    @Column(nullable = false)
    private int totalCount;

    @Column(nullable = false)
    private int queuedCount;

    @Column(nullable = false)
    private int processingCount;

    @Column(nullable = false)
    private int succeededCount;

    @Column(nullable = false)
    private int failedCount;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    protected Job() {
    }

    public Job(
        Long projectId,
        Long userId,
        String preset,
        Map<String, String> presetParameters,
        boolean debug,
        JobPriority priority,
        int totalCount
    ) {
        this.projectId = projectId;
        this.userId = userId;
        this.preset = preset;
        this.presetParameters = new LinkedHashMap<>(presetParameters);
        this.debug = debug;
        this.priority = priority;
        this.status = JobStatus.CREATED;
        this.totalCount = totalCount;
    }

    public static Job create(
        Long projectId,
        Long userId,
        String preset,
        Map<String, String> presetParameters,
        boolean debug,
        JobPriority priority,
        int totalCount
    ) {
        return new Job(projectId, userId, preset, presetParameters, debug, priority, totalCount);
    }

    public void markQueued(int queuedCount) {
        this.status = JobStatus.QUEUED;
        this.queuedCount = queuedCount;
    }

    public void requestCancel() {
        if (status == JobStatus.SUCCEEDED || status == JobStatus.FAILED || status == JobStatus.CANCELLED) {
            return;
        }
        this.status = JobStatus.CANCEL_REQUESTED;
    }

    public void markRetrying(int queuedCount) {
        this.status = JobStatus.RETRYING;
        this.queuedCount = queuedCount;
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getPreset() {
        return preset;
    }

    public Map<String, String> getPresetParameters() {
        return Map.copyOf(presetParameters);
    }

    public boolean isDebug() {
        return debug;
    }

    public JobPriority getPriority() {
        return priority;
    }

    public JobStatus getStatus() {
        return status;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getQueuedCount() {
        return queuedCount;
    }

    public int getProcessingCount() {
        return processingCount;
    }

    public int getSucceededCount() {
        return succeededCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}
