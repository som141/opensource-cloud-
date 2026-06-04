package com.moonju.preprocess.worker.domain.preprocess.step;

import com.moonju.preprocess.worker.domain.preprocess.model.ImageMatHolder;
import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessContext;
import java.util.Locale;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;

@Component
public class MorphologyCleanupStep implements PreprocessStep {

    private static final int DEFAULT_KERNEL_SIZE = 2;

    @Override
    public PreprocessStepName name() {
        return PreprocessStepName.MORPHOLOGY_CLEANUP;
    }

    @Override
    public void execute(PreprocessContext context) {
        if (context.decodedImage().isEmpty()) {
            context.recordStep(name(), "Decoded image is not available; morphology cleanup is deferred.");
            return;
        }

        ImageMatHolder holder = context.decodedImage().orElseThrow();
        if (!holder.loaded()) {
            context.recordStep(name(), "Decoded image is not loaded; morphology cleanup is deferred.");
            return;
        }

        String mode = context.parameters().getOrDefault("morphologyMode", "open")
            .toLowerCase(Locale.ROOT);
        if ("none".equals(mode) || "off".equals(mode) || "false".equals(mode)) {
            context.recordStep(name(), "Morphology cleanup skipped: disabled by preset parameters.");
            return;
        }

        if (!isSupportedMode(mode)) {
            context.recordFallback(name(), "Unsupported morphology mode: " + mode, "open");
            mode = "open";
        }

        int kernelSize = kernelSize(context);
        Mat grayscale = toGrayscale(holder.mat());
        Mat inverted = new Mat();
        Core.bitwise_not(grayscale, inverted);
        Mat cleanedInverted = applyCleanup(inverted, mode, kernelSize);
        Mat cleaned = new Mat();
        Core.bitwise_not(cleanedInverted, cleaned);

        release(grayscale, inverted, cleanedInverted);

        ImageMatHolder cleanedHolder = ImageMatHolder.decoded(holder.sourceObjectKey(), cleaned);
        context.storeDecodedImage(cleanedHolder);
        context.recordStep(
            name(),
            "Morphology cleanup applied: mode="
                + mode
                + ", kernelSize="
                + kernelSize
        );
    }

    private Mat applyCleanup(Mat invertedBinary, String mode, int kernelSize) {
        Mat kernel = Imgproc.getStructuringElement(
            Imgproc.MORPH_RECT,
            new Size(kernelSize, kernelSize)
        );
        Mat result = invertedBinary.clone();
        try {
            if ("open".equals(mode) || "open_close".equals(mode)) {
                Mat opened = new Mat();
                Imgproc.morphologyEx(result, opened, Imgproc.MORPH_OPEN, kernel);
                result.release();
                result = opened;
            }
            if ("close".equals(mode) || "open_close".equals(mode)) {
                Mat closed = new Mat();
                Imgproc.morphologyEx(result, closed, Imgproc.MORPH_CLOSE, kernel);
                result.release();
                result = closed;
            }
            return result;
        } finally {
            kernel.release();
        }
    }

    private Mat toGrayscale(Mat source) {
        Mat grayscale = new Mat();
        if (source.channels() == 1) {
            source.copyTo(grayscale);
        } else if (source.channels() == 3) {
            Imgproc.cvtColor(source, grayscale, Imgproc.COLOR_BGR2GRAY);
        } else if (source.channels() == 4) {
            Imgproc.cvtColor(source, grayscale, Imgproc.COLOR_BGRA2GRAY);
        } else {
            throw new IllegalStateException("Unsupported channel layout for morphology cleanup: "
                + source.channels());
        }
        return grayscale;
    }

    private boolean isSupportedMode(String mode) {
        return "open".equals(mode) || "close".equals(mode) || "open_close".equals(mode);
    }

    private int kernelSize(PreprocessContext context) {
        String configured = context.parameters().get("morphologyKernelSize");
        if (configured == null || configured.isBlank()) {
            return DEFAULT_KERNEL_SIZE;
        }
        return Math.max(1, Integer.parseInt(configured));
    }

    private void release(Mat... mats) {
        for (Mat mat : mats) {
            mat.release();
        }
    }
}
