package com.moonju.preprocess.api.domain.job.entity;

import com.moonju.preprocess.api.domain.job.exception.UnsupportedJobItemArtifactTypeException;
import java.util.Locale;
import java.util.function.Function;

public enum JobItemArtifactType {
    PROCESSED(JobItem::getProcessedObjectKey),
    PREVIEW(JobItem::getPreviewObjectKey),
    REPORT(JobItem::getReportObjectKey);

    private final Function<JobItem, String> objectKeyExtractor;

    JobItemArtifactType(Function<JobItem, String> objectKeyExtractor) {
        this.objectKeyExtractor = objectKeyExtractor;
    }

    public static JobItemArtifactType from(String value) {
        try {
            return JobItemArtifactType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new UnsupportedJobItemArtifactTypeException();
        }
    }

    public String objectKey(JobItem item) {
        return objectKeyExtractor.apply(item);
    }

    public String apiValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
