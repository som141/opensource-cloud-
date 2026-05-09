package com.moonju.preprocess.api.infra.storage;

import java.time.Duration;

public record PresignedDownloadCommand(
    String objectKey,
    Duration expiresIn
) {
}
