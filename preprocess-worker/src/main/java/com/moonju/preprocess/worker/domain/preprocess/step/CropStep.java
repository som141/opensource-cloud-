package com.moonju.preprocess.worker.domain.preprocess.step;

import com.moonju.preprocess.worker.domain.preprocess.model.CropBounds;
import com.moonju.preprocess.worker.domain.preprocess.model.ImageMatHolder;
import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessContext;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;

@Component
public class CropStep implements PreprocessStep {

    private static final int DEFAULT_CROP_MARGIN_PIXELS = 4;
    private static final int MIN_FOREGROUND_POINTS = 20;
    private static final double MIN_REMAINING_AREA_RATIO = 0.05;

    @Override
    public PreprocessStepName name() {
        return PreprocessStepName.CROP;
    }

    @Override
    public void execute(PreprocessContext context) {
        if (context.decodedImage().isEmpty()) {
            context.recordStep(name(), "Decoded image is not available; crop is deferred.");
            return;
        }

        ImageMatHolder holder = context.decodedImage().orElseThrow();
        if (!holder.loaded()) {
            context.recordStep(name(), "Decoded image is not loaded; crop is deferred.");
            return;
        }

        Mat grayscale = toGrayscale(holder.mat());
        Mat foregroundMask = new Mat();
        Imgproc.threshold(grayscale, foregroundMask, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);

        MatOfPoint foregroundPoints = new MatOfPoint();
        Core.findNonZero(foregroundMask, foregroundPoints);
        if (foregroundPoints.rows() < MIN_FOREGROUND_POINTS) {
            release(grayscale, foregroundMask, foregroundPoints);
            context.recordFallback(name(), "Not enough foreground points for crop.", "skip");
            context.recordStep(name(), "Crop skipped: insufficient foreground points.");
            return;
        }

        Rect detectedBounds = Imgproc.boundingRect(foregroundPoints);
        release(grayscale, foregroundMask, foregroundPoints);

        Rect cropRect = expandAndClamp(detectedBounds, holder.width(), holder.height(), cropMarginPixels(context));
        if (!isValidCrop(cropRect, holder.width(), holder.height())) {
            context.recordFallback(name(), "Detected crop bounds are too small or invalid.", "skip");
            context.recordStep(name(), "Crop skipped: invalid bounds.");
            return;
        }

        if (cropRect.width == holder.width() && cropRect.height == holder.height()) {
            context.recordStep(name(), "Crop skipped: detected bounds already cover the full image.");
            return;
        }

        Mat croppedView = new Mat(holder.mat(), cropRect);
        Mat cropped = croppedView.clone();
        croppedView.release();

        ImageMatHolder croppedHolder = ImageMatHolder.decoded(holder.sourceObjectKey(), cropped);
        context.storeDecodedImage(croppedHolder);
        CropBounds bounds = new CropBounds(cropRect.x, cropRect.y, cropRect.width, cropRect.height);
        context.recordStep(
            name(),
            "Crop applied: x="
                + bounds.x()
                + ", y="
                + bounds.y()
                + ", width="
                + bounds.width()
                + ", height="
                + bounds.height()
        );
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
            throw new IllegalStateException("Unsupported channel layout for crop: " + source.channels());
        }
        return grayscale;
    }

    private Rect expandAndClamp(Rect bounds, int imageWidth, int imageHeight, int marginPixels) {
        int x = Math.max(0, bounds.x - marginPixels);
        int y = Math.max(0, bounds.y - marginPixels);
        int maxX = Math.min(imageWidth, bounds.x + bounds.width + marginPixels);
        int maxY = Math.min(imageHeight, bounds.y + bounds.height + marginPixels);
        return new Rect(x, y, maxX - x, maxY - y);
    }

    private boolean isValidCrop(Rect cropRect, int imageWidth, int imageHeight) {
        if (cropRect.width <= 0 || cropRect.height <= 0) {
            return false;
        }
        double remainingAreaRatio = (cropRect.width * (double) cropRect.height) / (imageWidth * (double) imageHeight);
        return remainingAreaRatio >= MIN_REMAINING_AREA_RATIO;
    }

    private int cropMarginPixels(PreprocessContext context) {
        String configured = context.parameters().get("cropMarginPixels");
        if (configured == null || configured.isBlank()) {
            return DEFAULT_CROP_MARGIN_PIXELS;
        }
        return Math.max(0, Integer.parseInt(configured));
    }

    private void release(Mat... mats) {
        for (Mat mat : mats) {
            mat.release();
        }
    }
}
