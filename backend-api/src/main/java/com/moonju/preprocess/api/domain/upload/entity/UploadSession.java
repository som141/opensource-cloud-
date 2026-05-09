package com.moonju.preprocess.api.domain.upload.entity;

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
@Table(name = "upload_sessions")
public class UploadSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UploadSessionStatus status;

    @Column(nullable = false)
    private int expectedFileCount;

    @Column(nullable = false)
    private long expectedTotalSizeBytes;

    private LocalDateTime completedAt;

    private LocalDateTime cancelledAt;

    protected UploadSession() {
    }

    public UploadSession(
        Long projectId,
        Long userId,
        UploadSessionStatus status,
        int expectedFileCount,
        long expectedTotalSizeBytes
    ) {
        this.projectId = projectId;
        this.userId = userId;
        this.status = status;
        this.expectedFileCount = expectedFileCount;
        this.expectedTotalSizeBytes = expectedTotalSizeBytes;
    }

    public static UploadSession create(
        Long projectId,
        Long userId,
        int expectedFileCount,
        long expectedTotalSizeBytes
    ) {
        return new UploadSession(
            projectId,
            userId,
            UploadSessionStatus.CREATED,
            expectedFileCount,
            expectedTotalSizeBytes
        );
    }

    public void markUploadUrlIssued() {
        if (status == UploadSessionStatus.CREATED) {
            status = UploadSessionStatus.UPLOAD_URL_ISSUED;
        }
    }

    public void complete() {
        status = UploadSessionStatus.COMPLETED;
        completedAt = LocalDateTime.now();
    }

    public void cancel() {
        status = UploadSessionStatus.CANCELLED;
        cancelledAt = LocalDateTime.now();
    }

    public boolean isOpen() {
        return status == UploadSessionStatus.CREATED || status == UploadSessionStatus.UPLOAD_URL_ISSUED;
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

    public UploadSessionStatus getStatus() {
        return status;
    }

    public int getExpectedFileCount() {
        return expectedFileCount;
    }

    public long getExpectedTotalSizeBytes() {
        return expectedTotalSizeBytes;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }
}
