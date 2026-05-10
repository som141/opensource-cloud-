package com.moonju.preprocess.worker.domain.preprocess.step;

import static org.assertj.core.api.Assertions.assertThat;

import com.moonju.preprocess.worker.domain.preprocess.model.ImageMatHolder;
import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessContext;
import com.moonju.preprocess.worker.infra.opencv.OpenCvLoader;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

class DeskewStepTests {

    private final DeskewStep step = new DeskewStep();

    @BeforeAll
    static void loadOpenCv() {
        new OpenCvLoader().loadIfPresent();
    }

    @Test
    void appliesDeskewWhenSkewAngleIsDetected() {
        PreprocessContext context = context(Map.of("maxDeskewAngle", "40"));
        ImageMatHolder skewedHolder = ImageMatHolder.decoded("originals/skewed.png", skewedDocument());
        context.storeDecodedImage(skewedHolder);

        step.execute(context);

        ImageMatHolder deskewedHolder = context.decodedImage().orElseThrow();
        assertThat(skewedHolder.released()).isTrue();
        assertThat(deskewedHolder.loaded()).isTrue();
        assertThat(deskewedHolder.colorSpace()).isEqualTo("BGR");
        assertThat(context.consumeStepNote(PreprocessStepName.DESKEW)).contains("Deskew applied");
        context.releaseDecodedImage();
    }

    @Test
    void skipsBlankImageAndRecordsFallback() {
        PreprocessContext context = context(Map.of());
        ImageMatHolder blankHolder = ImageMatHolder.decoded(
            "originals/blank.png",
            new Mat(60, 80, CvType.CV_8UC3, new Scalar(255, 255, 255))
        );
        context.storeDecodedImage(blankHolder);

        step.execute(context);

        assertThat(context.decodedImage().orElseThrow()).isSameAs(blankHolder);
        assertThat(blankHolder.released()).isFalse();
        assertThat(context.fallbackNotes()).hasSize(1);
        assertThat(context.fallbackNotes().getFirst().selectedStrategy()).isEqualTo("skip");
        assertThat(context.consumeStepNote(PreprocessStepName.DESKEW)).contains("insufficient");
        context.releaseDecodedImage();
    }

    @Test
    void defersWhenDecodedImageIsMissing() {
        PreprocessContext context = context(Map.of());

        step.execute(context);

        assertThat(context.consumeStepNote(PreprocessStepName.DESKEW)).contains("not available");
    }

    private Mat skewedDocument() {
        Mat document = new Mat(80, 120, CvType.CV_8UC3, new Scalar(255, 255, 255));
        Imgproc.rectangle(document, new Point(20, 35), new Point(100, 45), new Scalar(0, 0, 0), -1);
        Mat rotated = new Mat();
        Mat rotationMatrix = Imgproc.getRotationMatrix2D(new Point(60, 40), 12.0, 1.0);
        Imgproc.warpAffine(
            document,
            rotated,
            rotationMatrix,
            new Size(120, 80),
            Imgproc.INTER_LINEAR,
            Core.BORDER_CONSTANT,
            new Scalar(255, 255, 255)
        );
        document.release();
        rotationMatrix.release();
        return rotated;
    }

    private PreprocessContext context(Map<String, String> parameters) {
        return new PreprocessContext(
            3L,
            1L,
            2L,
            "originals/project/scan.png",
            "LOW_CONTRAST_SCAN",
            parameters,
            false
        );
    }
}
