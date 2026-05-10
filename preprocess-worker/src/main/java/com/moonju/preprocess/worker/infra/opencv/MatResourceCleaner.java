package com.moonju.preprocess.worker.infra.opencv;

import com.moonju.preprocess.worker.domain.preprocess.model.ImageMatHolder;
import org.springframework.stereotype.Component;

@Component
public class MatResourceCleaner {

    public void release(ImageMatHolder imageMatHolder) {
        if (imageMatHolder != null) {
            imageMatHolder.release();
        }
    }
}
