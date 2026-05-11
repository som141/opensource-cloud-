package com.moonju.preprocess.worker.domain.preprocess.step;

import com.moonju.preprocess.worker.domain.preprocess.model.ImageMatHolder;
import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessContext;
import java.util.Locale;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;

@Component
public class BinarizationStep implements PreprocessStep {

    private static final int DEFAULT_ADAPTIVE_BLOCK_SIZE = 31;
    private static final double DEFAULT_ADAPTIVE_C = 7.0;

    @Override
    public PreprocessStepName name() {
        return PreprocessStepName.BINARIZATION;
    }

    @Override
    public void execute(PreprocessContext context) {
        if (context.decodedImage().isEmpty()) {
            context.recordStep(name(), "Decoded image is not available; binarization is deferred.");
            return;
        }

        ImageMatHolder holder = context.decodedImage().orElseThrow();
        if (!holder.loaded()) {
            context.recordStep(name(), "Decoded image is not loaded; binarization is deferred.");
            return;
        }

        Mat grayscale = toGrayscale(holder.mat());
        Mat binary = new Mat();
        String mode = context.parameters().getOrDefault("binarizationMode", "otsu")
            .toLowerCase(Locale.ROOT);
        if ("adaptive".equals(mode)) {
            int blockSize = positiveOddInt(
                context.parameters().get("adaptiveBlockSize"),
                DEFAULT_ADAPTIVE_BLOCK_SIZE
            );
            double c = doubleValue(context.parameters().get("adaptiveC"), DEFAULT_ADAPTIVE_C);
            Imgproc.adaptiveThreshold(
                grayscale,
                binary,
                255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY,
                blockSize,
                c
            );
            replaceImage(context, holder, grayscale, binary, "Binarized image: mode=adaptive, blockSize="
                + blockSize
                + ", c="
                + c);
            return;
        }

        if (!"otsu".equals(mode)) {
            context.recordFallback(name(), "Unsupported binarization mode: " + mode, "otsu");
        }

        Imgproc.threshold(grayscale, binary, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
        replaceImage(context, holder, grayscale, binary, "Binarized image: mode=otsu.");
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
            throw new IllegalStateException("Unsupported channel layout for binarization: " + source.channels());
        }
        return grayscale;
    }

    private void replaceImage(
        PreprocessContext context,
        ImageMatHolder holder,
        Mat grayscale,
        Mat binary,
        String note
    ) {
        grayscale.release();
        ImageMatHolder binaryHolder = ImageMatHolder.decoded(holder.sourceObjectKey(), binary);
        context.storeDecodedImage(binaryHolder);
        context.recordStep(name(), note);
    }

    private int positiveOddInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        int parsed = Math.max(3, Integer.parseInt(value));
        return parsed % 2 == 0 ? parsed + 1 : parsed;
    }

    private double doubleValue(String value, double defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Double.parseDouble(value);
    }
}
