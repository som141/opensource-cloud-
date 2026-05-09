package com.moonju.preprocess.api.infra.storage;

public interface ObjectStoragePort {

    boolean exists(String objectKey);
}
