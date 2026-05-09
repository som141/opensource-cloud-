package com.moonju.preprocess.api.domain.image.entity;

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
@Table(name = "image_artifacts")
public class ImageArtifact extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long imageId;

    private Long jobId;

    private Long jobItemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ImageArtifactType type;

    @Column(nullable = false, length = 1000)
    private String objectKey;

    @Column(nullable = false, length = 100)
    private String contentType;

    private Long sizeBytes;

    @Column(length = 100)
    private String debugStep;

    protected ImageArtifact() {
    }

    public ImageArtifact(
        Long imageId,
        Long jobId,
        Long jobItemId,
        ImageArtifactType type,
        String objectKey,
        String contentType,
        Long sizeBytes,
        String debugStep
    ) {
        this.imageId = imageId;
        this.jobId = jobId;
        this.jobItemId = jobItemId;
        this.type = type;
        this.objectKey = objectKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.debugStep = debugStep;
    }

    public static ImageArtifact original(Image image) {
        return new ImageArtifact(
            image.getId(),
            null,
            null,
            ImageArtifactType.ORIGINAL,
            image.getOriginalObjectKey(),
            image.getContentType(),
            image.getSizeBytes(),
            null
        );
    }

    public Long getId() {
        return id;
    }

    public Long getImageId() {
        return imageId;
    }

    public Long getJobId() {
        return jobId;
    }

    public Long getJobItemId() {
        return jobItemId;
    }

    public ImageArtifactType getType() {
        return type;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public String getDebugStep() {
        return debugStep;
    }
}
