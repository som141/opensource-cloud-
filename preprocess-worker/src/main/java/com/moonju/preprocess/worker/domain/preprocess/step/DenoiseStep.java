package com.moonju.preprocess.worker.domain.preprocess.step;

import com.moonju.preprocess.worker.domain.preprocess.model.ImageMatHolder;
import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessContext;
import java.util.Locale;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;

@Component
public class DenoiseStep implements PreprocessStep {

    private static final int DEFAULT_MEDIAN_KERNEL_SIZE = 3;
    private static final int DEFAULT_BILATERAL_DIAMETER = 5;
    private static final double DEFAULT_BILATERAL_SIGMA_COLOR = 25.0;
    private static final double DEFAULT_BILATERAL_SIGMA_RANGE = 75.0;

    @Override
    public PreprocessStepName name() {
        return PreprocessStepName.DENOISE;
    }

    @Override
    public void execute(PreprocessContext context) {
        if (context.decodedImage().isEmpty()) {
            context.recordStep(name(), "Decoded image is not available; denoise is deferred.");
            return;
        }

        ImageMatHolder holder = context.decodedImage().orElseThrow();
        if (!holder.loaded()) {
            context.recordStep(name(), "Decoded image is not loaded; denoise is deferred.");
            return;
        }

        String mode = context.parameters().getOrDefault("denoiseMode", "median")
            .toLowerCase(Locale.ROOT);
        if ("none".equals(mode) || "off".equals(mode) || "false".equals(mode)) {
            context.recordStep(name(), "Denoise skipped: disabled by preset parameters.");
            return;
        }

        if (holder.mat().channels() != 1 && holder.mat().channels() != 3) {
            throw new IllegalStateException("Unsupported channel layout for denoise: " + holder.mat().channels());
        }

        Mat denoised = new Mat();
        if ("bilateral".equals(mode)) {
            int diameter = positiveOddInt(context.parameters().get("denoiseDiameter"), DEFAULT_BILATERAL_DIAMETER);
            double sigmaColor = positiveDouble(
                context.parameters().get("denoiseSigmaColor"),
                DEFAULT_BILATERAL_SIGMA_COLOR
            );
            double sigmaRange = positiveDouble(
                context.parameters().get("denoiseSigmaRange"),
                DEFAULT_BILATERAL_SIGMA_RANGE
            );
            Imgproc.bilateralFilter(holder.mat(), denoised, diameter, sigmaColor, sigmaRange);
            replaceImage(
                context,
                holder,
                denoised,
                "Denoise applied: mode=bilateral, diameter="
                    + diameter
                    + ", sigmaColor="
                    + sigmaColor
                    + ", sigmaRange="
                    + sigmaRange
            );
            return;
        }

        if (!"median".equals(mode)) {
            context.recordFallback(name(), "Unsupported denoise mode: " + mode, "median");
        }

        int kernelSize = positiveOddInt(context.parameters().get("denoiseKernelSize"), DEFAULT_MEDIAN_KERNEL_SIZE);
        Imgproc.medianBlur(holder.mat(), denoised, kernelSize);
        replaceImage(context, holder, denoised, "Denoise applied: mode=median, kernelSize=" + kernelSize);
    }

    private void replaceImage(PreprocessContext context, ImageMatHolder holder, Mat result, String note) {
        ImageMatHolder denoisedHolder = ImageMatHolder.decoded(holder.sourceObjectKey(), result);
        context.storeDecodedImage(denoisedHolder);
        context.recordStep(name(), note);
    }

    private int positiveOddInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        int parsed = Math.max(3, Integer.parseInt(value));
        return parsed % 2 == 0 ? parsed + 1 : parsed;
    }

    private double positiveDouble(String value, double defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Math.max(0.1, Double.parseDouble(value));
    }
}
