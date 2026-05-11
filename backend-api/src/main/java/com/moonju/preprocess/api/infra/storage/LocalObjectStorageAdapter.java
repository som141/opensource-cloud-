package com.moonju.preprocess.api.infra.storage;

public class LocalObjectStorageAdapter implements ObjectStoragePort {

    @Override
    public boolean exists(String objectKey) {
        return true;
    }
}
