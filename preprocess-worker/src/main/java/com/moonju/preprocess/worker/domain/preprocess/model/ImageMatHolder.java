package com.moonju.preprocess.worker.domain.preprocess.model;

public record ImageMatHolder(
    String sourceObjectKey,
    String colorSpace,
    int width,
    int height,
    boolean placeholder
) {

    public static ImageMatHolder placeholder(String sourceObjectKey) {
        return new ImageMatHolder(sourceObjectKey, "UNLOADED", 0, 0, true);
    }

    public boolean loaded() {
        return !placeholder;
    }
}
