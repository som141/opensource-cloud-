package com.moonju.preprocess.worker.domain.preprocess.exception;

public class PresetNotSupportedException extends RuntimeException {

    public PresetNotSupportedException(String presetName) {
        super("Unsupported preprocess preset: " + presetName);
    }
}
