package com.moonju.preprocess.worker.domain.preprocess.step;

import com.moonju.preprocess.worker.domain.preprocess.model.ImageMatHolder;
import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessContext;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;

@Component
public class SharpenStep implements PreprocessStep {

    private static final double DEFAULT_SHARPEN_AMOUNT = 0.6;
    private static final double DEFAULT_SHARPEN_SIGMA = 1.0;

    @Override
    public PreprocessStepName name() {
        return PreprocessStepName.OPTIONAL_SHARPEN;
    }

    @Override
    public void execute(PreprocessContext context) {
        if (context.decodedImage().isEmpty()) {
            context.recordStep(name(), "Decoded image is not available; optional sharpen is deferred.");
            return;
        }

        ImageMatHolder holder = context.decodedImage().orElseThrow();
        if (!holder.loaded()) {
            context.recordStep(name(), "Decoded image is not loaded; optional sharpen is deferred.");
            return;
        }

        if (!enabled(context)) {
            context.recordStep(name(), "Optional sharpen skipped: disabled by preset parameters.");
            return;
        }

        if (holder.mat().channels() != 1 && holder.mat().channels() != 3) {
            throw new IllegalStateException("Unsupported channel layout for sharpen: " + holder.mat().channels());
        }

        double amount = positiveDouble(context.parameters().get("sharpenAmount"), DEFAULT_SHARPEN_AMOUNT);
        double sigma = positiveDouble(context.parameters().get("sharpenSigma"), DEFAULT_SHARPEN_SIGMA);
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(holder.mat(), blurred, new Size(0, 0), sigma);

        Mat sharpened = new Mat();
        Core.addWeighted(holder.mat(), 1.0 + amount, blurred, -amount, 0.0, sharpened);
        blurred.release();

        ImageMatHolder sharpenedHolder = ImageMatHolder.decoded(holder.sourceObjectKey(), sharpened);
        context.storeDecodedImage(sharpenedHolder);
        context.recordStep(
            name(),
            "Optional sharpen applied: method=unsharpMask, amount="
                + amount
                + ", sigma="
                + sigma
        );
    }

    private boolean enabled(PreprocessContext context) {
        String configured = context.parameters().get("sharpen");
        if (configured == null || configured.isBlank()) {
            return false;
        }
        return "true".equalsIgnoreCase(configured)
            || "on".equalsIgnoreCase(configured)
            || "yes".equalsIgnoreCase(configured);
    }

    private double positiveDouble(String value, double defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Math.max(0.1, Double.parseDouble(value));
    }
}
