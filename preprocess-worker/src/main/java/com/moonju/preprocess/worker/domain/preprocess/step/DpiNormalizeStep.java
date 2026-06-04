package com.moonju.preprocess.worker.domain.preprocess.step;

import com.moonju.preprocess.worker.domain.preprocess.model.DpiInfo;
import com.moonju.preprocess.worker.domain.preprocess.model.ImageMatHolder;
import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessContext;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;

@Component
public class DpiNormalizeStep implements PreprocessStep {

    private static final int DEFAULT_TARGET_DPI = 300;
    private static final double MIN_SCALE = 0.75;
    private static final double MAX_SCALE = 2.5;
    private static final double NO_OP_SCALE_DELTA = 0.01;

    @Override
    public PreprocessStepName name() {
        return PreprocessStepName.DPI_NORMALIZE;
    }

    @Override
    public void execute(PreprocessContext context) {
        if (context.decodedImage().isEmpty()) {
            context.recordStep(name(), "Decoded image is not available; DPI normalization is deferred.");
            return;
        }

        ImageMatHolder holder = context.decodedImage().orElseThrow();
        if (!holder.loaded()) {
            context.recordStep(name(), "Decoded image is not loaded; DPI normalization is deferred.");
            return;
        }

        DpiInfo dpiInfo = dpiInfo(context.parameters(), holder);
        if (!dpiInfo.sourceKnown()) {
            context.recordFallback(name(), "Source DPI metadata is missing.", "skip");
            context.recordStep(name(), "DPI normalization skipped: source DPI is unknown.");
            return;
        }

        double scaleX = clampScale(dpiInfo.targetDpi() / (double) dpiInfo.xDpi());
        double scaleY = clampScale(dpiInfo.targetDpi() / (double) dpiInfo.yDpi());
        if (isNoOp(scaleX) && isNoOp(scaleY)) {
            context.recordStep(
                name(),
                "DPI already normalized: sourceDpiX="
                    + dpiInfo.xDpi()
                    + ", sourceDpiY="
                    + dpiInfo.yDpi()
                    + ", targetDpi="
                    + dpiInfo.targetDpi()
            );
            return;
        }

        int targetWidth = Math.max(1, (int) Math.round(holder.width() * scaleX));
        int targetHeight = Math.max(1, (int) Math.round(holder.height() * scaleY));
        Mat normalized = new Mat();
        Imgproc.resize(
            holder.mat(),
            normalized,
            new Size(targetWidth, targetHeight),
            0,
            0,
            interpolation(scaleX, scaleY)
        );

        ImageMatHolder normalizedHolder = ImageMatHolder.decoded(holder.sourceObjectKey(), normalized);
        context.storeDecodedImage(normalizedHolder);
        context.recordStep(
            name(),
            "DPI normalized: sourceDpiX="
                + dpiInfo.xDpi()
                + ", sourceDpiY="
                + dpiInfo.yDpi()
                + ", targetDpi="
                + dpiInfo.targetDpi()
                + ", width="
                + normalizedHolder.width()
                + ", height="
                + normalizedHolder.height()
        );
    }

    private DpiInfo dpiInfo(Map<String, String> parameters, ImageMatHolder holder) {
        int targetDpi = parsePositiveInt(parameters.get("targetDpi")).orElse(DEFAULT_TARGET_DPI);
        OptionalInt sameAxisDpi = firstPositiveInt(parameters, "sourceDpi", "dpi", "xDpi");
        int sourceDpiX = firstPositiveInt(parameters, "sourceDpiX", "dpiX").orElseGet(() -> sameAxisDpi.orElse(0));
        int sourceDpiY = firstPositiveInt(parameters, "sourceDpiY", "dpiY", "yDpi")
            .orElseGet(() -> sameAxisDpi.orElse(0));
        if (sourceDpiX <= 0 || sourceDpiY <= 0) {
            OptionalInt estimatedX = estimateDpi(holder.width(), parameters.get("referenceWidthInches"));
            OptionalInt estimatedY = estimateDpi(holder.height(), parameters.get("referenceHeightInches"));
            sourceDpiX = sourceDpiX > 0 ? sourceDpiX : estimatedX.orElse(0);
            sourceDpiY = sourceDpiY > 0 ? sourceDpiY : estimatedY.orElse(0);
        }
        if (sourceDpiX <= 0 || sourceDpiY <= 0) {
            OptionalInt fallbackSourceDpi = parsePositiveInt(parameters.get("fallbackSourceDpi"));
            sourceDpiX = sourceDpiX > 0 ? sourceDpiX : fallbackSourceDpi.orElse(0);
            sourceDpiY = sourceDpiY > 0 ? sourceDpiY : fallbackSourceDpi.orElse(0);
        }
        return new DpiInfo(sourceDpiX, sourceDpiY, targetDpi);
    }

    private OptionalInt estimateDpi(int pixels, String referenceInches) {
        OptionalDouble parsedReferenceInches = parsePositiveDouble(referenceInches);
        if (parsedReferenceInches.isEmpty()) {
            return OptionalInt.empty();
        }
        int estimated = (int) Math.round(pixels / parsedReferenceInches.getAsDouble());
        return estimated > 0 ? OptionalInt.of(estimated) : OptionalInt.empty();
    }

    private OptionalInt firstPositiveInt(Map<String, String> parameters, String... keys) {
        for (String key : keys) {
            OptionalInt parsed = parsePositiveInt(parameters.get(key));
            if (parsed.isPresent()) {
                return parsed;
            }
        }
        return OptionalInt.empty();
    }

    private OptionalInt parsePositiveInt(String value) {
        if (value == null || value.isBlank()) {
            return OptionalInt.empty();
        }
        int parsed = Integer.parseInt(value);
        if (parsed <= 0) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(parsed);
    }

    private OptionalDouble parsePositiveDouble(String value) {
        if (value == null || value.isBlank()) {
            return OptionalDouble.empty();
        }
        double parsed = Double.parseDouble(value);
        if (parsed <= 0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(parsed);
    }

    private double clampScale(double scale) {
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
    }

    private boolean isNoOp(double scale) {
        return Math.abs(scale - 1.0) < NO_OP_SCALE_DELTA;
    }

    private int interpolation(double scaleX, double scaleY) {
        if (scaleX > 1.0 || scaleY > 1.0) {
            return Imgproc.INTER_CUBIC;
        }
        return Imgproc.INTER_AREA;
    }
}
