package com.moonju.preprocess.worker.domain.preprocess.model;

public record DpiInfo(
    int xDpi,
    int yDpi,
    int targetDpi
) {

    public static DpiInfo unknown(int targetDpi) {
        return new DpiInfo(0, 0, targetDpi);
    }

    public boolean sourceKnown() {
        return xDpi > 0 && yDpi > 0;
    }
}
