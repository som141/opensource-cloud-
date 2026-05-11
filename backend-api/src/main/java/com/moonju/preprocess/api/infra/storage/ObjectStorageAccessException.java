package com.moonju.preprocess.api.infra.storage;

public class ObjectStorageAccessException extends RuntimeException {

    public ObjectStorageAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
