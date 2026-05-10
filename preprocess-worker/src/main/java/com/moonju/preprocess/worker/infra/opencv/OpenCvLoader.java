package com.moonju.preprocess.worker.infra.opencv;

import java.util.concurrent.atomic.AtomicBoolean;
import nu.pattern.OpenCV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OpenCvLoader {

    private static final Logger log = LoggerFactory.getLogger(OpenCvLoader.class);

    private final AtomicBoolean loaded = new AtomicBoolean(false);

    public void load() {
        if (loaded.get()) {
            return;
        }
        synchronized (this) {
            if (loaded.get()) {
                return;
            }
            try {
                OpenCV.loadLocally();
                loaded.set(true);
                log.info("OpenCV native library loaded.");
            } catch (RuntimeException | UnsatisfiedLinkError exception) {
                throw new OpenCvLoadFailedException(exception);
            }
        }
    }

    public boolean loadIfPresent() {
        load();
        return loaded.get();
    }

    public boolean loaded() {
        return loaded.get();
    }
}
