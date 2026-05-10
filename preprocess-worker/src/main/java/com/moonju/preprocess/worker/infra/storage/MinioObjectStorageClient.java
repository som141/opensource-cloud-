package com.moonju.preprocess.worker.infra.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MinioObjectStorageClient implements ObjectStoragePort {

    private static final Logger log = LoggerFactory.getLogger(MinioObjectStorageClient.class);

    private final StorageProperties properties;

    public MinioObjectStorageClient(StorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public void prepareDownload(String objectKey) {
        log.info(
            "Object storage download skeleton: endpoint={}, bucket={}, objectKey={}",
            properties.getEndpoint(),
            properties.getBucket(),
            objectKey
        );
    }
}
