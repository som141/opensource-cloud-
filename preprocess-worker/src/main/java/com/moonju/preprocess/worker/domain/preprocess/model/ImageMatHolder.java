package com.moonju.preprocess.worker.domain.preprocess.model;

import org.opencv.core.Mat;

public final class ImageMatHolder implements AutoCloseable {

    private final String sourceObjectKey;
    private final String colorSpace;
    private final int width;
    private final int height;
    private final boolean placeholder;
    private final Mat mat;
    private boolean released;

    private ImageMatHolder(
        String sourceObjectKey,
        String colorSpace,
        int width,
        int height,
        boolean placeholder,
        Mat mat
    ) {
        this.sourceObjectKey = sourceObjectKey;
        this.colorSpace = colorSpace;
        this.width = width;
        this.height = height;
        this.placeholder = placeholder;
        this.mat = mat;
    }

    public static ImageMatHolder placeholder(String sourceObjectKey) {
        return new ImageMatHolder(sourceObjectKey, "UNLOADED", 0, 0, true, null);
    }

    public static ImageMatHolder decoded(String sourceObjectKey, Mat mat) {
        if (mat == null || mat.empty()) {
            throw new IllegalArgumentException("Decoded OpenCV Mat is empty.");
        }
        return new ImageMatHolder(
            sourceObjectKey,
            colorSpaceOf(mat.channels()),
            mat.cols(),
            mat.rows(),
            false,
            mat
        );
    }

    private static String colorSpaceOf(int channels) {
        return switch (channels) {
            case 1 -> "GRAY";
            case 3 -> "BGR";
            case 4 -> "BGRA";
            default -> "CHANNELS_" + channels;
        };
    }

    public String sourceObjectKey() {
        return sourceObjectKey;
    }

    public String colorSpace() {
        return colorSpace;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean placeholder() {
        return placeholder;
    }

    public Mat mat() {
        return mat;
    }

    public boolean loaded() {
        return !placeholder && !released && mat != null && !mat.empty();
    }

    public boolean released() {
        return released;
    }

    public void release() {
        if (!released && mat != null) {
            mat.release();
        }
        released = true;
    }

    @Override
    public void close() {
        release();
    }
}
