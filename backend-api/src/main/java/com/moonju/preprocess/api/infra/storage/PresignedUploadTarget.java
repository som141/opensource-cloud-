package com.moonju.preprocess.api.infra.storage;

import java.time.Instant;
import java.util.Map;

public record PresignedUploadTarget(
    String objectKey,
    String uploadUrl,
    Instant expiresAt,
    Map<String, String> requiredHeaders
) {
}
