package com.moonju.preprocess.worker.infra.storage;

public class ObjectStorageDownloadFailedException extends RuntimeException {

    public ObjectStorageDownloadFailedException(String objectKey, Throwable cause) {
        super("Failed to download object from storage: " + objectKey, cause);
    }
}
