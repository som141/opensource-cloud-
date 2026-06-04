package com.moonju.preprocess.worker.domain.preprocess.step;

import com.moonju.preprocess.worker.domain.preprocess.model.ImageMatHolder;
import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessContext;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;

@Component
public class ColorNormalizeStep implements PreprocessStep {

    @Override
    public PreprocessStepName name() {
        return PreprocessStepName.COLOR_NORMALIZE;
    }

    @Override
    public void execute(PreprocessContext context) {
        if (context.decodedImage().isEmpty()) {
            context.recordStep(name(), "Decoded image is not available; color normalization is deferred.");
            return;
        }

        ImageMatHolder holder = context.decodedImage().orElseThrow();
        if (!holder.loaded()) {
            context.recordStep(name(), "Decoded image is not loaded; color normalization is deferred.");
            return;
        }

        String beforeColorSpace = holder.colorSpace();
        if (grayscaleEnabled(context)) {
            if ("GRAY".equals(beforeColorSpace)) {
                context.recordStep(name(), "Color already normalized: GRAY.");
                return;
            }

            Mat normalized = new Mat();
            if ("BGR".equals(beforeColorSpace)) {
                Imgproc.cvtColor(holder.mat(), normalized, Imgproc.COLOR_BGR2GRAY);
            } else if ("BGRA".equals(beforeColorSpace)) {
                Imgproc.cvtColor(holder.mat(), normalized, Imgproc.COLOR_BGRA2GRAY);
            } else {
                throw new IllegalStateException("Unsupported color space for normalization: " + beforeColorSpace);
            }

            ImageMatHolder normalizedHolder = ImageMatHolder.decoded(holder.sourceObjectKey(), normalized);
            context.storeDecodedImage(normalizedHolder);
            context.recordStep(name(), "Color normalized: " + beforeColorSpace + " -> GRAY");
            return;
        }

        if ("BGR".equals(beforeColorSpace)) {
            context.recordStep(name(), "Color already normalized: BGR.");
            return;
        }

        Mat normalized = new Mat();
        if ("GRAY".equals(beforeColorSpace)) {
            Imgproc.cvtColor(holder.mat(), normalized, Imgproc.COLOR_GRAY2BGR);
        } else if ("BGRA".equals(beforeColorSpace)) {
            Imgproc.cvtColor(holder.mat(), normalized, Imgproc.COLOR_BGRA2BGR);
        } else {
            throw new IllegalStateException("Unsupported color space for normalization: " + beforeColorSpace);
        }

        ImageMatHolder normalizedHolder = ImageMatHolder.decoded(holder.sourceObjectKey(), normalized);
        context.storeDecodedImage(normalizedHolder);
        context.recordStep(name(), "Color normalized: " + beforeColorSpace + " -> " + normalizedHolder.colorSpace());
    }

    private boolean grayscaleEnabled(PreprocessContext context) {
        String configured = context.parameters().get("grayscale");
        if (configured == null || configured.isBlank()) {
            return false;
        }
        return "true".equalsIgnoreCase(configured)
            || "on".equalsIgnoreCase(configured)
            || "yes".equalsIgnoreCase(configured);
    }
}
