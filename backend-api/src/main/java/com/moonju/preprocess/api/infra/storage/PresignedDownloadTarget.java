package com.moonju.preprocess.api.infra.storage;

import java.time.Instant;
import java.util.Map;

public record PresignedDownloadTarget(
    String objectKey,
    String downloadUrl,
    Instant expiresAt,
    Map<String, String> requiredHeaders
) {
}
