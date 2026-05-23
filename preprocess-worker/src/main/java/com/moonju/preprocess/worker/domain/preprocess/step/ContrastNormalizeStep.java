package com.moonju.preprocess.worker.domain.preprocess.step;

import com.moonju.preprocess.worker.domain.preprocess.model.ImageMatHolder;
import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessContext;
import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;

@Component
public class ContrastNormalizeStep implements PreprocessStep {

    private static final double DEFAULT_CLIP_LIMIT = 2.0;
    private static final int DEFAULT_TILE_GRID_SIZE = 8;

    @Override
    public PreprocessStepName name() {
        return PreprocessStepName.CONTRAST_NORMALIZE;
    }

    @Override
    public void execute(PreprocessContext context) {
        if (context.decodedImage().isEmpty()) {
            context.recordStep(name(), "Decoded image is not available; contrast normalization is deferred.");
            return;
        }

        ImageMatHolder holder = context.decodedImage().orElseThrow();
        if (!holder.loaded()) {
            context.recordStep(name(), "Decoded image is not loaded; contrast normalization is deferred.");
            return;
        }

        double clipLimit = positiveDouble(context.parameters().get("contrastClipLimit"), DEFAULT_CLIP_LIMIT);
        int tileGridSize = positiveInt(context.parameters().get("contrastTileGridSize"), DEFAULT_TILE_GRID_SIZE);
        Mat normalized = normalizeContrast(holder.mat(), clipLimit, tileGridSize);
        ImageMatHolder normalizedHolder = ImageMatHolder.decoded(holder.sourceObjectKey(), normalized);
        context.storeDecodedImage(normalizedHolder);
        context.recordStep(
            name(),
            "Contrast normalized: method=CLAHE, clipLimit="
                + clipLimit
                + ", tileGridSize="
                + tileGridSize
        );
    }

    private Mat normalizeContrast(Mat source, double clipLimit, int tileGridSize) {
        CLAHE clahe = Imgproc.createCLAHE(clipLimit, new Size(tileGridSize, tileGridSize));
        try {
            if (source.channels() == 1) {
                Mat normalized = new Mat();
                clahe.apply(source, normalized);
                return normalized;
            }

            if (source.channels() != 3) {
                throw new IllegalStateException("Unsupported channel layout for contrast normalization: "
                    + source.channels());
            }

            Mat lab = new Mat();
            Imgproc.cvtColor(source, lab, Imgproc.COLOR_BGR2Lab);
            List<Mat> channels = new ArrayList<>(3);
            Core.split(lab, channels);
            Mat normalizedLuminance = new Mat();
            clahe.apply(channels.get(0), normalizedLuminance);
            Mat originalLuminance = channels.set(0, normalizedLuminance);
            originalLuminance.release();
            Core.merge(channels, lab);

            Mat normalized = new Mat();
            Imgproc.cvtColor(lab, normalized, Imgproc.COLOR_Lab2BGR);
            release(lab);
            release(channels);
            return normalized;
        } finally {
            clahe.collectGarbage();
        }
    }

    private double positiveDouble(String value, double defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Math.max(0.1, Double.parseDouble(value));
    }

    private int positiveInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Math.max(1, Integer.parseInt(value));
    }

    private void release(Mat mat) {
        mat.release();
    }

    private void release(List<Mat> mats) {
        for (Mat mat : mats) {
            mat.release();
        }
    }
}
