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

@Entity
@Table(name = "upload_session_files")
public class UploadSessionFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long uploadSessionId;

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 255)
    private String originalFileName;

    @Column(nullable = false, length = 1000)
    private String objectKey;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false, length = 64)
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UploadFileStatus status;

    protected UploadSessionFile() {
    }

    public UploadSessionFile(
        Long uploadSessionId,
        Long projectId,
        String originalFileName,
        String objectKey,
        String contentType,
        long sizeBytes,
        String checksumSha256,
        UploadFileStatus status
    ) {
        this.uploadSessionId = uploadSessionId;
        this.projectId = projectId;
        this.originalFileName = originalFileName;
        this.objectKey = objectKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.checksumSha256 = checksumSha256;
        this.status = status;
    }

    public static UploadSessionFile issued(
        Long uploadSessionId,
        Long projectId,
        String originalFileName,
        String objectKey,
        String contentType,
        long sizeBytes,
        String checksumSha256
    ) {
        return new UploadSessionFile(
            uploadSessionId,
            projectId,
            originalFileName,
            objectKey,
            contentType,
            sizeBytes,
            checksumSha256,
            UploadFileStatus.UPLOAD_URL_ISSUED
        );
    }

    public void markUploaded() {
        status = UploadFileStatus.UPLOADED;
    }

    public boolean isUploaded() {
        return status == UploadFileStatus.UPLOADED;
    }

    public Long getId() {
        return id;
    }

    public Long getUploadSessionId() {
        return uploadSessionId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public UploadFileStatus getStatus() {
        return status;
    }
}
