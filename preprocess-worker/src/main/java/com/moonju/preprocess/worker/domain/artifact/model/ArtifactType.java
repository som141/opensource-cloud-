package com.moonju.preprocess.worker.domain.artifact.model;

public enum ArtifactType {
    PROCESSED_IMAGE("processed.png", "image/png", false),
    PREVIEW_IMAGE("preview.png", "image/png", false),
    PROCESSING_REPORT("processing-report.json", "application/json", false),
    DEBUG_IMAGE("", "image/png", true);

    private final String defaultFileName;
    private final String contentType;
    private final boolean debug;

    ArtifactType(String defaultFileName, String contentType, boolean debug) {
        this.defaultFileName = defaultFileName;
        this.contentType = contentType;
        this.debug = debug;
    }

    public String defaultFileName() {
        return defaultFileName;
    }

    public String contentType() {
        return contentType;
    }

    public boolean debug() {
        return debug;
    }
}
