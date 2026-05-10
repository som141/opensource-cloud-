package com.moonju.preprocess.worker.domain.preprocess.exception;

public class ImageDecodeFailedException extends RuntimeException {

    public ImageDecodeFailedException(String message) {
        super(message);
    }

    public ImageDecodeFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
