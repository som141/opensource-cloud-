package com.moonju.preprocess.worker.infra.opencv;

public class OpenCvLoadFailedException extends RuntimeException {

    public OpenCvLoadFailedException(Throwable cause) {
        super("Failed to load OpenCV native library.", cause);
    }
}
