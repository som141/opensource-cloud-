package com.moonju.preprocess.worker.domain.preprocess.model;

public record CropBounds(
    int x,
    int y,
    int width,
    int height
) {

    public boolean applied() {
        return width > 0 && height > 0;
    }

    public long area() {
        return (long) width * height;
    }
}
