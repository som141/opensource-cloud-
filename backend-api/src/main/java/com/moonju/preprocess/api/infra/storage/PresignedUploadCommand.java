package com.moonju.preprocess.api.infra.storage;

import java.time.Duration;

public record PresignedUploadCommand(
    String objectKey,
    String contentType,
    long sizeBytes,
    Duration expiresIn
) {
}
