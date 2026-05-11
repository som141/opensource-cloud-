package com.moonju.preprocess.api.infra.storage;

import java.time.Instant;
import java.util.Map;

public class LocalPresignedDownloadUrlGenerator implements PresignedDownloadUrlGenerator {

    @Override
    public PresignedDownloadTarget generateDownloadUrl(PresignedDownloadCommand command) {
        Instant expiresAt = Instant.now().plus(command.expiresIn());
        return new PresignedDownloadTarget(
            command.objectKey(),
            "http://localhost:9000/local-download/" + command.objectKey(),
            expiresAt,
            Map.of()
        );
    }
}
