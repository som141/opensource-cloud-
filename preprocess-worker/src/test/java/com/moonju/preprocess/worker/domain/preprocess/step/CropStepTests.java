package com.moonju.preprocess.worker.domain.preprocess.step;

import static org.assertj.core.api.Assertions.assertThat;

import com.moonju.preprocess.worker.domain.preprocess.model.ImageMatHolder;
import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessContext;
import com.moonju.preprocess.worker.infra.opencv.OpenCvLoader;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

class CropStepTests {

    private final CropStep step = new CropStep();

    @BeforeAll
    static void loadOpenCv() {
        new OpenCvLoader().loadIfPresent();
    }

    @Test
    void cropsForegroundBoundsAndReleasesPreviousHolder() {
        PreprocessContext context = context(Map.of("cropMarginPixels", "2"));
        ImageMatHolder originalHolder = ImageMatHolder.decoded("originals/crop.png", documentWithBorder());
        context.storeDecodedImage(originalHolder);

        step.execute(context);

        ImageMatHolder croppedHolder = context.decodedImage().orElseThrow();
        assertThat(originalHolder.released()).isTrue();
        assertThat(croppedHolder.loaded()).isTrue();
        assertThat(croppedHolder.width()).isLessThan(100);
        assertThat(croppedHolder.height()).isLessThan(100);
        assertThat(croppedHolder.colorSpace()).isEqualTo("BGR");
        assertThat(context.consumeStepNote(PreprocessStepName.CROP)).contains("Crop applied");
        context.releaseDecodedImage();
    }

    @Test
    void skipsBlankImageAndRecordsFallback() {
        PreprocessContext context = context(Map.of());
        ImageMatHolder blankHolder = ImageMatHolder.decoded(
            "originals/blank.png",
            new Mat(80, 100, CvType.CV_8UC3, new Scalar(255, 255, 255))
        );
        context.storeDecodedImage(blankHolder);

        step.execute(context);

        assertThat(context.decodedImage().orElseThrow()).isSameAs(blankHolder);
        assertThat(blankHolder.released()).isFalse();
        assertThat(context.fallbackNotes()).hasSize(1);
        assertThat(context.fallbackNotes().getFirst().stepName()).isEqualTo("CROP");
        assertThat(context.fallbackNotes().getFirst().selectedStrategy()).isEqualTo("skip");
        assertThat(context.consumeStepNote(PreprocessStepName.CROP)).contains("insufficient");
        context.releaseDecodedImage();
    }

    @Test
    void defersWhenDecodedImageIsMissing() {
        PreprocessContext context = context(Map.of());

        step.execute(context);

        assertThat(context.consumeStepNote(PreprocessStepName.CROP)).contains("not available");
    }

    private Mat documentWithBorder() {
        Mat document = new Mat(100, 100, CvType.CV_8UC3, new Scalar(255, 255, 255));
        Imgproc.rectangle(document, new Point(25, 30), new Point(75, 70), new Scalar(0, 0, 0), -1);
        return document;
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
