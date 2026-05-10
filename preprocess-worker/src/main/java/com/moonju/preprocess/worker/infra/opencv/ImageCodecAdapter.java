package com.moonju.preprocess.worker.infra.opencv;

import com.moonju.preprocess.worker.domain.preprocess.model.ImageMatHolder;
import org.springframework.stereotype.Component;

@Component
public class ImageCodecAdapter {

    private final OpenCvLoader openCvLoader;

    public ImageCodecAdapter(OpenCvLoader openCvLoader) {
        this.openCvLoader = openCvLoader;
    }

    public ImageMatHolder decodePlaceholder(String objectKey) {
        openCvLoader.loadIfPresent();
        return ImageMatHolder.placeholder(objectKey);
    }
}
