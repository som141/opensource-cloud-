package com.moonju.preprocess.worker.domain.preprocess.step;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.moonju.preprocess.worker.domain.preprocess.model.ImageMatHolder;
import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessContext;
import com.moonju.preprocess.worker.infra.opencv.OpenCvLoader;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

class ColorNormalizeStepTests {

    private final ColorNormalizeStep step = new ColorNormalizeStep();

    @BeforeAll
    static void loadOpenCv() {
        new OpenCvLoader().loadIfPresent();
    }

    @Test
    void convertsGrayImageToBgrAndReleasesPreviousHolder() {
        PreprocessContext context = context();
        ImageMatHolder grayHolder = ImageMatHolder.decoded(
            "originals/gray.png",
            new Mat(2, 3, CvType.CV_8UC1, new Scalar(128))
        );
        context.storeDecodedImage(grayHolder);

        step.execute(context);

        ImageMatHolder normalizedHolder = context.decodedImage().orElseThrow();
        assertThat(grayHolder.released()).isTrue();
        assertThat(normalizedHolder.loaded()).isTrue();
        assertThat(normalizedHolder.width()).isEqualTo(3);
        assertThat(normalizedHolder.height()).isEqualTo(2);
        assertThat(normalizedHolder.colorSpace()).isEqualTo("BGR");
        assertThat(context.consumeStepNote(PreprocessStepName.COLOR_NORMALIZE)).isEqualTo("Color normalized: GRAY -> BGR");
        context.releaseDecodedImage();
    }

    @Test
    void convertsBgraImageToBgrAndReleasesPreviousHolder() {
        PreprocessContext context = context();
        ImageMatHolder bgraHolder = ImageMatHolder.decoded(
            "originals/bgra.png",
            new Mat(2, 3, CvType.CV_8UC4, new Scalar(10, 20, 30, 255))
        );
        context.storeDecodedImage(bgraHolder);

        step.execute(context);

        ImageMatHolder normalizedHolder = context.decodedImage().orElseThrow();
        assertThat(bgraHolder.released()).isTrue();
        assertThat(normalizedHolder.colorSpace()).isEqualTo("BGR");
        assertThat(context.consumeStepNote(PreprocessStepName.COLOR_NORMALIZE)).isEqualTo("Color normalized: BGRA -> BGR");
        context.releaseDecodedImage();
    }

    @Test
    void keepsBgrImageAsNoOp() {
        PreprocessContext context = context();
        ImageMatHolder bgrHolder = ImageMatHolder.decoded(
            "originals/bgr.png",
            new Mat(2, 3, CvType.CV_8UC3, new Scalar(10, 20, 30))
        );
        context.storeDecodedImage(bgrHolder);

        step.execute(context);

        assertThat(context.decodedImage().orElseThrow()).isSameAs(bgrHolder);
        assertThat(bgrHolder.released()).isFalse();
        assertThat(context.consumeStepNote(PreprocessStepName.COLOR_NORMALIZE)).isEqualTo("Color already normalized: BGR.");
        context.releaseDecodedImage();
    }

    @Test
    void defersWhenDecodedImageIsMissing() {
        PreprocessContext context = context();

        step.execute(context);

        assertThat(context.consumeStepNote(PreprocessStepName.COLOR_NORMALIZE))
            .contains("not available");
    }

    @Test
    void rejectsUnsupportedChannelLayout() {
        PreprocessContext context = context();
        ImageMatHolder holder = ImageMatHolder.decoded(
            "originals/unsupported.png",
            new Mat(2, 3, CvType.CV_8UC2, new Scalar(1, 2))
        );
        context.storeDecodedImage(holder);

        assertThatThrownBy(() -> step.execute(context))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Unsupported color space");
        context.releaseDecodedImage();
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
