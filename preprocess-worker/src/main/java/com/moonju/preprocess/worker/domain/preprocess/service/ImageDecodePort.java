package com.moonju.preprocess.worker.domain.preprocess.service;

import com.moonju.preprocess.worker.domain.preprocess.model.ImageMatHolder;

@FunctionalInterface
public interface ImageDecodePort {

    ImageMatHolder decode(String objectKey, byte[] imageBytes);
}
