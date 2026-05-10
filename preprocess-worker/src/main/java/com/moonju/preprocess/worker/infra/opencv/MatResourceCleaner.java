package com.moonju.preprocess.worker.infra.opencv;

import com.moonju.preprocess.worker.domain.preprocess.model.ImageMatHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MatResourceCleaner {

    private static final Logger log = LoggerFactory.getLogger(MatResourceCleaner.class);

    public void release(ImageMatHolder imageMatHolder) {
        log.debug("Mat resource cleanup skeleton invoked for {}", imageMatHolder.sourceObjectKey());
    }
}
