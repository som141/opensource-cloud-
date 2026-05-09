package com.moonju.preprocess.api.infra.storage;

public interface PresignedDownloadUrlGenerator {

    PresignedDownloadTarget generateDownloadUrl(PresignedDownloadCommand command);
}
