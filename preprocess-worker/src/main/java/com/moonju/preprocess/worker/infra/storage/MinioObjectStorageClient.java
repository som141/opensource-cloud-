package com.moonju.preprocess.worker.infra.storage;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.io.ByteArrayInputStream;
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

    @Override
    public void uploadBytes(String objectKey, byte[] content, String contentType) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content)) {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .contentType(contentType)
                    .stream(inputStream, (long) content.length, -1L)
                    .build()
            );
            log.info("Uploaded object to storage: bucket={}, objectKey={}, bytes={}",
                properties.getBucket(),
                objectKey,
                content.length
            );
        } catch (Exception exception) {
            throw new ObjectStorageUploadFailedException(objectKey, exception);
        }
    }
}
