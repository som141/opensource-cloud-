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

class DenoiseStepTests {

    private final DenoiseStep step = new DenoiseStep();

    @BeforeAll
    static void loadOpenCv() {
        new OpenCvLoader().loadIfPresent();
    }

    @Test
    void appliesMedianDenoiseAndReleasesPreviousHolder() {
        PreprocessContext context = context(Map.of("denoiseKernelSize", "3"));
        ImageMatHolder sourceHolder = ImageMatHolder.decoded("originals/noisy.png", noisyImage());
        context.storeDecodedImage(sourceHolder);

        step.execute(context);

        ImageMatHolder denoisedHolder = context.decodedImage().orElseThrow();
        assertThat(sourceHolder.released()).isTrue();
        assertThat(denoisedHolder.loaded()).isTrue();
        assertThat(denoisedHolder.width()).isEqualTo(5);
        assertThat(denoisedHolder.height()).isEqualTo(5);
        assertThat(denoisedHolder.colorSpace()).isEqualTo("BGR");
        assertThat(context.consumeStepNote(PreprocessStepName.DENOISE)).contains("mode=median");
        context.releaseDecodedImage();
    }

    @Test
    void skipsWhenDisabledByParameter() {
        PreprocessContext context = context(Map.of("denoiseMode", "none"));
        ImageMatHolder sourceHolder = ImageMatHolder.decoded("originals/clean.png", noisyImage());
        context.storeDecodedImage(sourceHolder);

        step.execute(context);

        assertThat(context.decodedImage().orElseThrow()).isSameAs(sourceHolder);
        assertThat(sourceHolder.released()).isFalse();
        assertThat(context.consumeStepNote(PreprocessStepName.DENOISE)).contains("disabled");
        context.releaseDecodedImage();
    }

    @Test
    void fallsBackToMedianWhenModeIsUnsupported() {
        PreprocessContext context = context(Map.of("denoiseMode", "unknown"));
        ImageMatHolder sourceHolder = ImageMatHolder.decoded("originals/noisy.png", noisyImage());
        context.storeDecodedImage(sourceHolder);

        step.execute(context);

        assertThat(context.fallbackNotes()).hasSize(1);
        assertThat(context.fallbackNotes().getFirst().stepName()).isEqualTo("DENOISE");
        assertThat(context.fallbackNotes().getFirst().selectedStrategy()).isEqualTo("median");
        assertThat(context.consumeStepNote(PreprocessStepName.DENOISE)).contains("mode=median");
        context.releaseDecodedImage();
    }

    @Test
    void appliesBilateralDenoiseWithDefaultImageTestSigmas() {
        PreprocessContext context = context(Map.of("denoiseMode", "bilateral"));
        ImageMatHolder sourceHolder = ImageMatHolder.decoded("originals/noisy.png", noisyImage());
        context.storeDecodedImage(sourceHolder);

        step.execute(context);

        assertThat(context.consumeStepNote(PreprocessStepName.DENOISE))
            .contains("mode=bilateral")
            .contains("diameter=7")
            .contains("sigmaColor=50.0")
            .contains("sigmaRange=50.0");
        context.releaseDecodedImage();
    }

    @Test
    void appliesBilateralDenoiseWithConfiguredSigmas() {
        PreprocessContext context = context(Map.of(
            "denoiseMode",
            "bilateral",
            "denoiseSigmaColor",
            "40.0",
            "denoiseSigmaRange",
            "60.0"
        ));
        ImageMatHolder sourceHolder = ImageMatHolder.decoded("originals/noisy.png", noisyImage());
        context.storeDecodedImage(sourceHolder);

        step.execute(context);

        assertThat(context.consumeStepNote(PreprocessStepName.DENOISE))
            .contains("sigmaColor=40.0")
            .contains("sigmaRange=60.0");
        context.releaseDecodedImage();
    }

    @Test
    void defersWhenDecodedImageIsMissing() {
        PreprocessContext context = context(Map.of());

        step.execute(context);

        assertThat(context.consumeStepNote(PreprocessStepName.DENOISE)).contains("not available");
    }

    private Mat noisyImage() {
        Mat image = new Mat(5, 5, CvType.CV_8UC3, new Scalar(180, 180, 180));
        image.put(2, 2, new byte[] {0, 0, 0});
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
