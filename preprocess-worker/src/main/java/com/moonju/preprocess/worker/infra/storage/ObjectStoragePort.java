package com.moonju.preprocess.worker.infra.storage;

public interface ObjectStoragePort {

    byte[] downloadBytes(String objectKey);
}
