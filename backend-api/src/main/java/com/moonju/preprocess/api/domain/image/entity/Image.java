package com.moonju.preprocess.api.domain.image.entity;

import com.moonju.preprocess.api.domain.upload.entity.UploadSession;
import com.moonju.preprocess.api.domain.upload.entity.UploadSessionFile;
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
@Table(name = "images")
public class Image extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Long uploadSessionId;

    @Column(nullable = false, unique = true)
    private Long uploadSessionFileId;

    @Column(nullable = false)
    private Long uploaderId;

    @Column(nullable = false, length = 255)
    private String originalFileName;

    @Column(nullable = false, length = 1000)
    private String originalObjectKey;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false, length = 64)
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImageFormat format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ImageStatus status;

    private Integer width;

    private Integer height;

    private Integer dpiX;

    private Integer dpiY;

    private LocalDateTime deletedAt;

    protected Image() {
    }

    public Image(
        Long projectId,
        Long uploadSessionId,
        Long uploadSessionFileId,
        Long uploaderId,
        String originalFileName,
        String originalObjectKey,
        String contentType,
        long sizeBytes,
        String checksumSha256,
        ImageFormat format,
        ImageStatus status
    ) {
        this.projectId = projectId;
        this.uploadSessionId = uploadSessionId;
        this.uploadSessionFileId = uploadSessionFileId;
        this.uploaderId = uploaderId;
        this.originalFileName = originalFileName;
        this.originalObjectKey = originalObjectKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.checksumSha256 = checksumSha256;
        this.format = format;
        this.status = status;
    }

    public static Image fromUpload(UploadSession uploadSession, UploadSessionFile file) {
        return new Image(
            uploadSession.getProjectId(),
            uploadSession.getId(),
            file.getId(),
            uploadSession.getUserId(),
            file.getOriginalFileName(),
            file.getObjectKey(),
            file.getContentType(),
            file.getSizeBytes(),
            file.getChecksumSha256(),
            ImageFormat.fromFileName(file.getOriginalFileName()),
            ImageStatus.UPLOADED
        );
    }

    public void delete() {
        status = ImageStatus.DELETED;
        deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return status == ImageStatus.DELETED;
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getUploadSessionId() {
        return uploadSessionId;
    }

    public Long getUploadSessionFileId() {
        return uploadSessionFileId;
    }

    public Long getUploaderId() {
        return uploaderId;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getOriginalObjectKey() {
        return originalObjectKey;
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

    public ImageFormat getFormat() {
        return format;
    }

    public ImageStatus getStatus() {
        return status;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public Integer getDpiX() {
        return dpiX;
    }

    public Integer getDpiY() {
        return dpiY;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
