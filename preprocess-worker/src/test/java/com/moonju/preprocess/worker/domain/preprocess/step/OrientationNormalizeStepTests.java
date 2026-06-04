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
import org.opencv.core.Scalar;

class OrientationNormalizeStepTests {

    private final OrientationNormalizeStep step = new OrientationNormalizeStep();

    @BeforeAll
    static void loadOpenCv() {
        new OpenCvLoader().loadIfPresent();
    }

    @Test
    void rotatesLandscapeImageToPortraitAndReleasesPreviousHolder() {
        PreprocessContext context = context();
        ImageMatHolder landscapeHolder = ImageMatHolder.decoded(
            "originals/landscape.png",
            new Mat(2, 5, CvType.CV_8UC3, new Scalar(255, 255, 255))
        );
        context.storeDecodedImage(landscapeHolder);

        step.execute(context);

        ImageMatHolder rotatedHolder = context.decodedImage().orElseThrow();
        assertThat(landscapeHolder.released()).isTrue();
        assertThat(rotatedHolder.width()).isEqualTo(2);
        assertThat(rotatedHolder.height()).isEqualTo(5);
        assertThat(rotatedHolder.colorSpace()).isEqualTo("BGR");
        assertThat(context.consumeStepNote(PreprocessStepName.ORIENTATION_NORMALIZE))
            .contains("landscape -> portrait")
            .contains("grossRotationDegrees=-90.0");
        context.releaseDecodedImage();
    }

    @Test
    void keepsSlightLandscapeImageAsNoOp() {
        PreprocessContext context = context();
        ImageMatHolder slightLandscapeHolder = ImageMatHolder.decoded(
            "originals/slight-landscape.png",
            new Mat(10, 11, CvType.CV_8UC3, new Scalar(255, 255, 255))
        );
        context.storeDecodedImage(slightLandscapeHolder);

        step.execute(context);

        assertThat(context.decodedImage().orElseThrow()).isSameAs(slightLandscapeHolder);
        assertThat(slightLandscapeHolder.released()).isFalse();
        assertThat(context.consumeStepNote(PreprocessStepName.ORIENTATION_NORMALIZE))
            .contains("already normalized");
        context.releaseDecodedImage();
    }

    @Test
    void keepsPortraitImageAsNoOp() {
        PreprocessContext context = context();
        ImageMatHolder portraitHolder = ImageMatHolder.decoded(
            "originals/portrait.png",
            new Mat(5, 2, CvType.CV_8UC3, new Scalar(255, 255, 255))
        );
        context.storeDecodedImage(portraitHolder);

        step.execute(context);

        assertThat(context.decodedImage().orElseThrow()).isSameAs(portraitHolder);
        assertThat(portraitHolder.released()).isFalse();
        assertThat(context.consumeStepNote(PreprocessStepName.ORIENTATION_NORMALIZE))
            .contains("already normalized");
        context.releaseDecodedImage();
    }

    @Test
    void defersWhenDecodedImageIsMissing() {
        PreprocessContext context = context();

        step.execute(context);

        assertThat(context.consumeStepNote(PreprocessStepName.ORIENTATION_NORMALIZE))
            .contains("not available");
    }

    private PreprocessContext context() {
        return new PreprocessContext(
            3L,
            1L,
            2L,
            "originals/project/scan.png",
            "LOW_CONTRAST_SCAN",
            Map.of(),
            false
        );
    }
}
