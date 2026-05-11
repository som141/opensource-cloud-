package com.moonju.preprocess.api.infra.storage;

import java.time.Instant;
import java.util.Map;

public class LocalPresignedUrlGenerator implements PresignedUrlGenerator {

    @Override
    public PresignedUploadTarget generateUploadUrl(PresignedUploadCommand command) {
        Instant expiresAt = Instant.now().plus(command.expiresIn());
        return new PresignedUploadTarget(
            command.objectKey(),
            "http://localhost:9000/local-presigned/" + command.objectKey(),
            expiresAt,
            Map.of("Content-Type", command.contentType())
        );
    }
}
