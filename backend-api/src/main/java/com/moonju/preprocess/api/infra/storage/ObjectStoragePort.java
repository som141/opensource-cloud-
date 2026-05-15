package com.moonju.preprocess.api.infra.storage;

public interface ObjectStoragePort {

    boolean exists(String objectKey);

    byte[] downloadBytes(String objectKey);

    void uploadBytes(String objectKey, byte[] bytes, String contentType);
}
