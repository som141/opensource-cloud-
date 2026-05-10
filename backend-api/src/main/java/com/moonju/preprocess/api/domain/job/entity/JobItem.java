package com.moonju.preprocess.api.domain.job.entity;

import com.moonju.preprocess.api.global.support.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_items")
public class JobItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long jobId;

    @Column(nullable = false)
    private Long imageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private JobItemStatus status;

    @Column(nullable = false)
    private int attempt;

    @Column(length = 100)
    private String workerId;

    @Column(length = 1000)
    private String processedObjectKey;

    @Column(length = 1000)
    private String previewObjectKey;

    @Column(length = 1000)
    private String reportObjectKey;

    @Column(length = 100)
    private String errorCode;

    @Column(length = 2000)
    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    protected JobItem() {
    }

    public JobItem(Long jobId, Long imageId, JobItemStatus status, int attempt) {
        this.jobId = jobId;
        this.imageId = imageId;
        this.status = status;
        this.attempt = attempt;
    }

    public static JobItem queued(Long jobId, Long imageId) {
        return new JobItem(jobId, imageId, JobItemStatus.QUEUED, 1);
    }

    public void requestCancel() {
        if (status == JobItemStatus.QUEUED || status == JobItemStatus.PENDING || status == JobItemStatus.RETRYING) {
            status = JobItemStatus.CANCELLED;
        }
    }

    public void retry() {
        if (status == JobItemStatus.FAILED || status == JobItemStatus.DEAD_LETTERED) {
            markRetrying();
        }
    }

    public boolean canRetry() {
        return status == JobItemStatus.FAILED || status == JobItemStatus.DEAD_LETTERED;
    }

    public void rerun() {
        if (status != JobItemStatus.PROCESSING) {
            markRetrying();
        }
    }

    private void markRetrying() {
        status = JobItemStatus.RETRYING;
        attempt++;
        errorCode = null;
        errorMessage = null;
        workerId = null;
        startedAt = null;
        completedAt = null;
    }

    public Long getId() {
        return id;
    }

    public Long getJobId() {
        return jobId;
    }

    public Long getImageId() {
        return imageId;
    }

    public JobItemStatus getStatus() {
        return status;
    }

    public int getAttempt() {
        return attempt;
    }

    public String getWorkerId() {
        return workerId;
    }

    public String getProcessedObjectKey() {
        return processedObjectKey;
    }

    public String getPreviewObjectKey() {
        return previewObjectKey;
    }

    public String getReportObjectKey() {
        return reportObjectKey;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}
