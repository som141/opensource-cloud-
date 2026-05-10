package com.moonju.preprocess.worker.infra.storage;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MinioObjectStorageClient implements ObjectStoragePort {

    private static final Logger log = LoggerFactory.getLogger(MinioObjectStorageClient.class);

    private final StorageProperties properties;
    private final MinioClient minioClient;

    public MinioObjectStorageClient(StorageProperties properties) {
        this.properties = properties;
        this.minioClient = MinioClient.builder()
            .endpoint(properties.getEndpoint())
            .credentials(properties.getAccessKey(), properties.getSecretKey())
            .build();
    }

    @Override
    public byte[] downloadBytes(String objectKey) {
        try (InputStream objectStream = minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(properties.getBucket())
                .object(objectKey)
                .build()
        )) {
            byte[] bytes = objectStream.readAllBytes();
            log.info("Downloaded object from storage: bucket={}, objectKey={}, bytes={}",
                properties.getBucket(),
                objectKey,
                bytes.length
            );
            return bytes;
        } catch (Exception exception) {
            throw new ObjectStorageDownloadFailedException(objectKey, exception);
        }
    }
}
