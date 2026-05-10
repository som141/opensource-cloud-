package com.moonju.preprocess.worker.domain.preprocess.step;

import com.moonju.preprocess.worker.domain.preprocess.model.ImageMatHolder;
import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessContext;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;

@Component
public class DeskewStep implements PreprocessStep {

    private static final double DEFAULT_MAX_DESKEW_ANGLE = 40.0;
    private static final double MIN_APPLY_ANGLE = 0.3;
    private static final int MIN_FOREGROUND_POINTS = 20;

    @Override
    public PreprocessStepName name() {
        return PreprocessStepName.DESKEW;
    }

    @Override
    public void execute(PreprocessContext context) {
        if (context.decodedImage().isEmpty()) {
            context.recordStep(name(), "Decoded image is not available; deskew is deferred.");
            return;
        }

        ImageMatHolder holder = context.decodedImage().orElseThrow();
        if (!holder.loaded()) {
            context.recordStep(name(), "Decoded image is not loaded; deskew is deferred.");
            return;
        }

        Mat grayscale = toGrayscale(holder.mat());
        Mat foregroundMask = new Mat();
        Imgproc.threshold(grayscale, foregroundMask, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);

        MatOfPoint foregroundPoints = new MatOfPoint();
        Core.findNonZero(foregroundMask, foregroundPoints);
        if (foregroundPoints.rows() < MIN_FOREGROUND_POINTS) {
            release(grayscale, foregroundMask, foregroundPoints);
            context.recordFallback(name(), "Not enough foreground points for deskew.", "skip");
            context.recordStep(name(), "Deskew skipped: insufficient foreground points.");
            return;
        }

        MatOfPoint2f points = new MatOfPoint2f(foregroundPoints.toArray());
        RotatedRect rectangle = Imgproc.minAreaRect(points);
        double correctionAngle = normalizeCorrectionAngle(rectangle.angle);
        release(grayscale, foregroundMask, foregroundPoints, points);

        double maxDeskewAngle = maxDeskewAngle(context);
        if (Math.abs(correctionAngle) > maxDeskewAngle) {
            context.recordFallback(name(), "Detected skew angle exceeds maximum.", "skip");
            context.recordStep(name(), "Deskew skipped: angle=" + round(correctionAngle) + " exceeds max=" + maxDeskewAngle);
            return;
        }

        if (Math.abs(correctionAngle) < MIN_APPLY_ANGLE) {
            context.recordStep(name(), "Deskew skipped: angle below threshold, angle=" + round(correctionAngle));
            return;
        }

        Mat deskewed = new Mat();
        Mat rotationMatrix = Imgproc.getRotationMatrix2D(
            new Point(holder.width() / 2.0, holder.height() / 2.0),
            correctionAngle,
            1.0
        );
        Imgproc.warpAffine(
            holder.mat(),
            deskewed,
            rotationMatrix,
            holder.mat().size(),
            Imgproc.INTER_LINEAR,
            Core.BORDER_REPLICATE,
            new Scalar(255, 255, 255)
        );
        rotationMatrix.release();

        ImageMatHolder deskewedHolder = ImageMatHolder.decoded(holder.sourceObjectKey(), deskewed);
        context.storeDecodedImage(deskewedHolder);
        context.recordStep(name(), "Deskew applied: method=minAreaRect, correctionAngle=" + round(correctionAngle));
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
            throw new IllegalStateException("Unsupported channel layout for deskew: " + source.channels());
        }
        return grayscale;
    }

    private double normalizeCorrectionAngle(double rawAngle) {
        double normalizedAngle = rawAngle;
        if (normalizedAngle > 45.0) {
            normalizedAngle -= 90.0;
        }
        if (normalizedAngle < -45.0) {
            normalizedAngle += 90.0;
        }
        return -normalizedAngle;
    }

    private double maxDeskewAngle(PreprocessContext context) {
        String configured = context.parameters().get("maxDeskewAngle");
        if (configured == null || configured.isBlank()) {
            return DEFAULT_MAX_DESKEW_ANGLE;
        }
        return Double.parseDouble(configured);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private void release(Mat... mats) {
        for (Mat mat : mats) {
            mat.release();
        }
    }
}
