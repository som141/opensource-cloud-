package com.moonju.preprocess.api.infra.storage;

public interface PresignedUrlGenerator {

    PresignedUploadTarget generateUploadUrl(PresignedUploadCommand command);
}
