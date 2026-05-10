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
import java.util.List;
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

    public void refreshProgress(List<JobItem> items, LocalDateTime now) {
        this.totalCount = items.size();
        this.queuedCount = countByStatus(items, JobItemStatus.PENDING, JobItemStatus.QUEUED, JobItemStatus.RETRYING);
        this.processingCount = countByStatus(items, JobItemStatus.PROCESSING);
        this.succeededCount = countByStatus(items, JobItemStatus.SUCCEEDED);
        this.failedCount = countByStatus(items, JobItemStatus.FAILED, JobItemStatus.DEAD_LETTERED);

        if (processingCount > 0) {
            status = JobStatus.RUNNING;
            if (startedAt == null) {
                startedAt = now;
            }
            completedAt = null;
            return;
        }

        if (queuedCount > 0) {
            if (status != JobStatus.CANCEL_REQUESTED) {
                status = JobStatus.QUEUED;
            }
            completedAt = null;
            return;
        }

        if (totalCount == 0 || succeededCount == totalCount) {
            status = JobStatus.SUCCEEDED;
            completedAt = now;
            return;
        }

        if (failedCount == totalCount) {
            status = JobStatus.FAILED;
            completedAt = now;
            return;
        }

        int terminalCount = succeededCount + failedCount + countByStatus(
            items,
            JobItemStatus.SKIPPED,
            JobItemStatus.CANCELLED
        );
        if (terminalCount == totalCount) {
            status = failedCount > 0 || succeededCount > 0 ? JobStatus.PARTIAL_SUCCEEDED : JobStatus.CANCELLED;
            completedAt = now;
        }
    }

    private int countByStatus(List<JobItem> items, JobItemStatus... statuses) {
        return (int) items.stream()
            .filter(item -> matchesAnyStatus(item, statuses))
            .count();
    }

    private boolean matchesAnyStatus(JobItem item, JobItemStatus[] statuses) {
        for (JobItemStatus status : statuses) {
            if (item.getStatus() == status) {
                return true;
            }
        }
        return false;
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
