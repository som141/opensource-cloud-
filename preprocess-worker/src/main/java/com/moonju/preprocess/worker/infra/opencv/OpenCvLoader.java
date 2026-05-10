package com.moonju.preprocess.worker.infra.opencv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OpenCvLoader {

    private static final Logger log = LoggerFactory.getLogger(OpenCvLoader.class);

    public boolean loadIfPresent() {
        log.info("OpenCV loader skeleton invoked; native library loading is not implemented yet.");
        return false;
    }
}
