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

class ContrastNormalizeStepTests {

    private final ContrastNormalizeStep step = new ContrastNormalizeStep();

    @BeforeAll
    static void loadOpenCv() {
        new OpenCvLoader().loadIfPresent();
    }

    @Test
    void normalizesBgrContrastAndReleasesPreviousHolder() {
        PreprocessContext context = context(Map.of("contrastClipLimit", "1.6", "contrastTileGridSize", "4"));
        ImageMatHolder sourceHolder = ImageMatHolder.decoded("originals/low-contrast.png", lowContrastBgrImage());
        context.storeDecodedImage(sourceHolder);

        step.execute(context);

        ImageMatHolder normalizedHolder = context.decodedImage().orElseThrow();
        assertThat(sourceHolder.released()).isTrue();
        assertThat(normalizedHolder.loaded()).isTrue();
        assertThat(normalizedHolder.width()).isEqualTo(8);
        assertThat(normalizedHolder.height()).isEqualTo(8);
        assertThat(normalizedHolder.colorSpace()).isEqualTo("BGR");
        assertThat(context.consumeStepNote(PreprocessStepName.CONTRAST_NORMALIZE)).contains("method=CLAHE");
        context.releaseDecodedImage();
    }

    @Test
    void normalizesGrayContrastAndKeepsGrayOutput() {
        PreprocessContext context = context(Map.of("contrastClipLimit", "1.4"));
        ImageMatHolder sourceHolder = ImageMatHolder.decoded(
            "originals/gray.png",
            new Mat(8, 8, CvType.CV_8UC1, new Scalar(120))
        );
        context.storeDecodedImage(sourceHolder);

        step.execute(context);

        ImageMatHolder normalizedHolder = context.decodedImage().orElseThrow();
        assertThat(sourceHolder.released()).isTrue();
        assertThat(normalizedHolder.colorSpace()).isEqualTo("GRAY");
        assertThat(context.consumeStepNote(PreprocessStepName.CONTRAST_NORMALIZE)).contains("clipLimit=1.4");
        context.releaseDecodedImage();
    }

    @Test
    void usesImageTestDefaultClipLimitWhenParameterIsMissing() {
        PreprocessContext context = context(Map.of());
        ImageMatHolder sourceHolder = ImageMatHolder.decoded("originals/default.png", lowContrastBgrImage());
        context.storeDecodedImage(sourceHolder);

        step.execute(context);

        assertThat(context.consumeStepNote(PreprocessStepName.CONTRAST_NORMALIZE)).contains("clipLimit=2.5");
        context.releaseDecodedImage();
    }

    @Test
    void skipsWhenDisabledByPresetParameter() {
        PreprocessContext context = context(Map.of("contrastNormalize", "false"));
        ImageMatHolder sourceHolder = ImageMatHolder.decoded("originals/default.png", lowContrastBgrImage());
        context.storeDecodedImage(sourceHolder);

        step.execute(context);

        assertThat(context.decodedImage().orElseThrow()).isSameAs(sourceHolder);
        assertThat(sourceHolder.released()).isFalse();
        assertThat(context.consumeStepNote(PreprocessStepName.CONTRAST_NORMALIZE)).contains("disabled");
        context.releaseDecodedImage();
    }

    @Test
    void defersWhenDecodedImageIsMissing() {
        PreprocessContext context = context(Map.of());

        step.execute(context);

        assertThat(context.consumeStepNote(PreprocessStepName.CONTRAST_NORMALIZE)).contains("not available");
    }

    private Mat lowContrastBgrImage() {
        Mat image = new Mat(8, 8, CvType.CV_8UC3, new Scalar(120, 120, 120));
        image.put(0, 0, new byte[] {100, 100, 100});
        image.put(7, 7, new byte[] {(byte) 140, (byte) 140, (byte) 140});
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
