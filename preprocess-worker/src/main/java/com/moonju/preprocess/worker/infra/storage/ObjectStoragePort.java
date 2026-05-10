package com.moonju.preprocess.worker.infra.storage;

public interface ObjectStoragePort {

    void prepareDownload(String objectKey);
}
