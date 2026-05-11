package com.moonju.preprocess.api.infra.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class MinioObjectStorageAdapterTests {

    @Test
    void createsBrowserReachablePresignedUploadAndDownloadUrls() {
        StorageProperties properties = new StorageProperties();
        properties.setEndpoint("http://minio:9000");
        properties.setPublicEndpoint("http://localhost:9000");
        properties.setAccessKey("minioadmin");
        properties.setSecretKey("minioadmin");
        properties.setBucket("image-preprocess-local");
        MinioObjectStorageAdapter adapter = new MinioObjectStorageAdapter(properties);

        PresignedUploadTarget uploadTarget = adapter.generateUploadUrl(new PresignedUploadCommand(
            "originals/1/1/token/scan.png",
            "image/png",
            1024L,
            Duration.ofMinutes(15)
        ));
        PresignedDownloadTarget downloadTarget = adapter.generateDownloadUrl(new PresignedDownloadCommand(
            "processed/1/1/1/processed.png",
            Duration.ofMinutes(10)
        ));

        assertThat(uploadTarget.uploadUrl()).startsWith("http://localhost:9000/image-preprocess-local/");
        assertThat(uploadTarget.requiredHeaders()).containsEntry("Content-Type", "image/png");
        assertThat(downloadTarget.downloadUrl()).startsWith("http://localhost:9000/image-preprocess-local/");
    }
}
