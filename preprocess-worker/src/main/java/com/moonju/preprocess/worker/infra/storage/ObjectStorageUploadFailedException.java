package com.moonju.preprocess.worker.infra.storage;

public class ObjectStorageUploadFailedException extends RuntimeException {

    public ObjectStorageUploadFailedException(String objectKey, Throwable cause) {
        super("Failed to upload object to storage: " + objectKey, cause);
    }
}
