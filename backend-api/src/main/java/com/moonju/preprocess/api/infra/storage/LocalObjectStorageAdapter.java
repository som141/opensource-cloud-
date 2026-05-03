package com.moonju.preprocess.api.infra.storage;

import org.springframework.stereotype.Component;

@Component
public class LocalObjectStorageAdapter implements ObjectStoragePort {

    @Override
    public boolean exists(String objectKey) {
        return true;
    }
}
