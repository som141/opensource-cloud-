package com.moonju.preprocess.worker.domain.preprocess.step;

import com.moonju.preprocess.worker.domain.preprocess.model.ImageMatHolder;
import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessContext;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.springframework.stereotype.Component;

@Component
public class OrientationNormalizeStep implements PreprocessStep {

    private static final double LANDSCAPE_ROTATION_THRESHOLD = 1.15;

    @Override
    public PreprocessStepName name() {
        return PreprocessStepName.ORIENTATION_NORMALIZE;
    }

    @Override
    public void execute(PreprocessContext context) {
        if (context.decodedImage().isEmpty()) {
            context.recordStep(name(), "Decoded image is not available; orientation normalization is deferred.");
            return;
        }

        ImageMatHolder holder = context.decodedImage().orElseThrow();
        if (!holder.loaded()) {
            context.recordStep(name(), "Decoded image is not loaded; orientation normalization is deferred.");
            return;
        }

        if (holder.width() <= holder.height() * LANDSCAPE_ROTATION_THRESHOLD) {
            context.recordStep(name(), "Orientation already normalized: portrait or square.");
            return;
        }

        Mat rotated = new Mat();
        Core.rotate(holder.mat(), rotated, Core.ROTATE_90_COUNTERCLOCKWISE);
        ImageMatHolder rotatedHolder = ImageMatHolder.decoded(holder.sourceObjectKey(), rotated);
        context.storeDecodedImage(rotatedHolder);
        context.recordStep(
            name(),
            "Orientation normalized: landscape -> portrait, grossRotationDegrees=-90.0, width="
                + rotatedHolder.width()
                + ", height="
                + rotatedHolder.height()
        );
    }
}
