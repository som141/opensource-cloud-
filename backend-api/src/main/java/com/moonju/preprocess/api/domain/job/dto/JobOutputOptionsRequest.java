package com.moonju.preprocess.api.domain.job.dto;

public record JobOutputOptionsRequest(
    Boolean saveProcessedImage,
    Boolean savePreview,
    Boolean saveReportJson,
    Boolean saveDebugArtifacts
) {

    public boolean shouldSaveProcessedImage() {
        return saveProcessedImage == null || saveProcessedImage;
    }

    public boolean shouldSavePreview() {
        return savePreview == null || savePreview;
    }

    public boolean shouldSaveReportJson() {
        return saveReportJson == null || saveReportJson;
    }

    public boolean shouldSaveDebugArtifacts() {
        return saveDebugArtifacts != null && saveDebugArtifacts;
    }
}
