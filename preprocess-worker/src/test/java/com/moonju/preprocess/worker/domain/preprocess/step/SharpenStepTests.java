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

class SharpenStepTests {

    private final SharpenStep step = new SharpenStep();

    @BeforeAll
    static void loadOpenCv() {
        new OpenCvLoader().loadIfPresent();
    }

    @Test
    void appliesSharpenWhenEnabledAndReleasesPreviousHolder() {
        PreprocessContext context = context(Map.of(
            "sharpen",
            "true",
            "sharpenAmount",
            "0.8",
            "sharpenSigma",
            "1.2"
        ));
        ImageMatHolder sourceHolder = ImageMatHolder.decoded("originals/processed.png", grayscaleImage());
        context.storeDecodedImage(sourceHolder);

        step.execute(context);

        ImageMatHolder sharpenedHolder = context.decodedImage().orElseThrow();
        assertThat(sourceHolder.released()).isTrue();
        assertThat(sharpenedHolder.loaded()).isTrue();
        assertThat(sharpenedHolder.width()).isEqualTo(8);
        assertThat(sharpenedHolder.height()).isEqualTo(8);
        assertThat(sharpenedHolder.colorSpace()).isEqualTo("GRAY");
        assertThat(context.consumeStepNote(PreprocessStepName.OPTIONAL_SHARPEN)).contains("unsharpMask");
        context.releaseDecodedImage();
    }

    @Test
    void usesTunedSharpenDefaultsWhenParametersAreMissing() {
        PreprocessContext context = context(Map.of("sharpen", "true"));
        ImageMatHolder sourceHolder = ImageMatHolder.decoded("originals/processed.png", grayscaleImage());
        context.storeDecodedImage(sourceHolder);

        step.execute(context);

        assertThat(context.consumeStepNote(PreprocessStepName.OPTIONAL_SHARPEN))
            .contains("amount=0.8")
            .contains("sigma=1.5");
        context.releaseDecodedImage();
    }

    @Test
    void skipsWhenDisabledByParameter() {
        PreprocessContext context = context(Map.of("sharpen", "false"));
        ImageMatHolder sourceHolder = ImageMatHolder.decoded("originals/processed.png", grayscaleImage());
        context.storeDecodedImage(sourceHolder);

        step.execute(context);

        assertThat(context.decodedImage().orElseThrow()).isSameAs(sourceHolder);
        assertThat(sourceHolder.released()).isFalse();
        assertThat(context.consumeStepNote(PreprocessStepName.OPTIONAL_SHARPEN)).contains("disabled");
        context.releaseDecodedImage();
    }

    @Test
    void skipsWhenParameterIsMissing() {
        PreprocessContext context = context(Map.of());
        ImageMatHolder sourceHolder = ImageMatHolder.decoded("originals/processed.png", grayscaleImage());
        context.storeDecodedImage(sourceHolder);

        step.execute(context);

        assertThat(context.decodedImage().orElseThrow()).isSameAs(sourceHolder);
        assertThat(sourceHolder.released()).isFalse();
        assertThat(context.consumeStepNote(PreprocessStepName.OPTIONAL_SHARPEN)).contains("disabled");
        context.releaseDecodedImage();
    }

    @Test
    void defersWhenDecodedImageIsMissing() {
        PreprocessContext context = context(Map.of());

        step.execute(context);

        assertThat(context.consumeStepNote(PreprocessStepName.OPTIONAL_SHARPEN)).contains("not available");
    }

    private Mat grayscaleImage() {
        Mat image = new Mat(8, 8, CvType.CV_8UC1, new Scalar(180));
        image.put(3, 3, new byte[] {60});
        return image;
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
