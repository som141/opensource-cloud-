package com.moonju.preprocess.api.infra.storage;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http.Method;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class MinioObjectStorageAdapter
    implements ObjectStoragePort, PresignedUrlGenerator, PresignedDownloadUrlGenerator {

    private final StorageProperties properties;
    private final MinioClient internalClient;
    private final MinioClient publicClient;

    public MinioObjectStorageAdapter(StorageProperties properties) {
        this.properties = properties;
        this.internalClient = MinioClient.builder()
            .endpoint(properties.getEndpoint())
            .credentials(properties.getAccessKey(), properties.getSecretKey())
            .region(properties.getRegion())
            .build();
        this.publicClient = MinioClient.builder()
            .endpoint(properties.getPublicEndpoint())
            .credentials(properties.getAccessKey(), properties.getSecretKey())
            .region(properties.getRegion())
            .build();
    }

    @Override
    public boolean exists(String objectKey) {
        try {
            internalClient.statObject(StatObjectArgs.builder()
                .bucket(properties.getBucket())
                .object(objectKey)
                .build());
            return true;
        } catch (ErrorResponseException exception) {
            String code = exception.errorResponse().code();
            if ("NoSuchKey".equals(code) || "NoSuchObject".equals(code)) {
                return false;
            }
            throw new ObjectStorageAccessException("Object storage stat failed: " + objectKey, exception);
        } catch (Exception exception) {
            throw new ObjectStorageAccessException("Object storage stat failed: " + objectKey, exception);
        }
    }

    @Override
    public PresignedUploadTarget generateUploadUrl(PresignedUploadCommand command) {
        Map<String, String> headers = Map.of("Content-Type", command.contentType());
        try {
            String url = publicClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.PUT)
                .bucket(properties.getBucket())
                .object(command.objectKey())
                .expiry(expirySeconds(command), TimeUnit.SECONDS)
                .extraHeaders(headers)
                .build());
            return new PresignedUploadTarget(
                command.objectKey(),
                url,
                Instant.now().plus(command.expiresIn()),
                headers
            );
        } catch (Exception exception) {
            throw new ObjectStorageAccessException("Presigned upload URL generation failed: " + command.objectKey(),
                exception);
        }
    }

    @Override
    public PresignedDownloadTarget generateDownloadUrl(PresignedDownloadCommand command) {
        try {
            String url = publicClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(properties.getBucket())
                .object(command.objectKey())
                .expiry(expirySeconds(command), TimeUnit.SECONDS)
                .build());
            return new PresignedDownloadTarget(
                command.objectKey(),
                url,
                Instant.now().plus(command.expiresIn()),
                Map.of()
            );
        } catch (Exception exception) {
            throw new ObjectStorageAccessException("Presigned download URL generation failed: " + command.objectKey(),
                exception);
        }
    }

    private int expirySeconds(PresignedUploadCommand command) {
        return Math.toIntExact(command.expiresIn().toSeconds());
    }

    private int expirySeconds(PresignedDownloadCommand command) {
        return Math.toIntExact(command.expiresIn().toSeconds());
    }
}
