package com.moonju.preprocess.api.domain.job.entity;

import java.util.Locale;

public enum JobItemArtifactDownloadType {
    PROCESSED,
    PREVIEW,
    REPORT;

    public static JobItemArtifactDownloadType from(String value) {
        try {
            return JobItemArtifactDownloadType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalArgumentException("Unsupported job item artifact type.");
        }
    }

    public String objectKey(JobItem item) {
        return switch (this) {
            case PROCESSED -> item.getProcessedObjectKey();
            case PREVIEW -> item.getPreviewObjectKey();
            case REPORT -> item.getReportObjectKey();
        };
    }
}
