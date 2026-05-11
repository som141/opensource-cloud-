package com.moonju.preprocess.worker.infra.storage;

public interface ObjectStoragePort {

    byte[] downloadBytes(String objectKey);

    void uploadBytes(String objectKey, byte[] content, String contentType);
}
