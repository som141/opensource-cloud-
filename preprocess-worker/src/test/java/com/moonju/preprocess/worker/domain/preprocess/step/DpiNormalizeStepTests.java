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

class DpiNormalizeStepTests {

    private final DpiNormalizeStep step = new DpiNormalizeStep();

    @BeforeAll
    static void loadOpenCv() {
        new OpenCvLoader().loadIfPresent();
    }

    @Test
    void scalesImageToTargetDpiAndReleasesPreviousHolder() {
        PreprocessContext context = context(Map.of("sourceDpi", "150", "targetDpi", "300"));
        ImageMatHolder sourceHolder = ImageMatHolder.decoded(
            "originals/dpi.png",
            new Mat(5, 10, CvType.CV_8UC3, new Scalar(255, 255, 255))
        );
        context.storeDecodedImage(sourceHolder);

        step.execute(context);

        ImageMatHolder normalizedHolder = context.decodedImage().orElseThrow();
        assertThat(sourceHolder.released()).isTrue();
        assertThat(normalizedHolder.width()).isEqualTo(20);
        assertThat(normalizedHolder.height()).isEqualTo(10);
        assertThat(normalizedHolder.colorSpace()).isEqualTo("BGR");
        assertThat(context.consumeStepNote(PreprocessStepName.DPI_NORMALIZE)).contains("DPI normalized");
        context.releaseDecodedImage();
    }

    @Test
    void keepsImageAsNoOpWhenSourceAlreadyMatchesTarget() {
        PreprocessContext context = context(Map.of("sourceDpiX", "300", "sourceDpiY", "300", "targetDpi", "300"));
        ImageMatHolder sourceHolder = ImageMatHolder.decoded(
            "originals/noop.png",
            new Mat(5, 10, CvType.CV_8UC3, new Scalar(255, 255, 255))
        );
        context.storeDecodedImage(sourceHolder);

        step.execute(context);

        assertThat(context.decodedImage().orElseThrow()).isSameAs(sourceHolder);
        assertThat(sourceHolder.released()).isFalse();
        assertThat(context.consumeStepNote(PreprocessStepName.DPI_NORMALIZE)).contains("already normalized");
        context.releaseDecodedImage();
    }

    @Test
    void skipsWhenSourceDpiIsMissingAndRecordsFallback() {
        PreprocessContext context = context(Map.of("targetDpi", "300"));
        ImageMatHolder sourceHolder = ImageMatHolder.decoded(
            "originals/missing-dpi.png",
            new Mat(5, 10, CvType.CV_8UC3, new Scalar(255, 255, 255))
        );
        context.storeDecodedImage(sourceHolder);

        step.execute(context);

        assertThat(context.decodedImage().orElseThrow()).isSameAs(sourceHolder);
        assertThat(sourceHolder.released()).isFalse();
        assertThat(context.fallbackNotes()).hasSize(1);
        assertThat(context.fallbackNotes().getFirst().stepName()).isEqualTo("DPI_NORMALIZE");
        assertThat(context.fallbackNotes().getFirst().selectedStrategy()).isEqualTo("skip");
        assertThat(context.consumeStepNote(PreprocessStepName.DPI_NORMALIZE)).contains("unknown");
        context.releaseDecodedImage();
    }

    @Test
    void usesFallbackSourceDpiWhenMetadataIsMissing() {
        PreprocessContext context = context(Map.of("targetDpi", "300", "fallbackSourceDpi", "150"));
        ImageMatHolder sourceHolder = ImageMatHolder.decoded(
            "originals/fallback-dpi.png",
            new Mat(5, 10, CvType.CV_8UC3, new Scalar(255, 255, 255))
        );
        context.storeDecodedImage(sourceHolder);

        step.execute(context);

        ImageMatHolder normalizedHolder = context.decodedImage().orElseThrow();
        assertThat(sourceHolder.released()).isTrue();
        assertThat(normalizedHolder.width()).isEqualTo(20);
        assertThat(normalizedHolder.height()).isEqualTo(10);
        assertThat(context.consumeStepNote(PreprocessStepName.DPI_NORMALIZE)).contains("sourceDpiX=150");
        context.releaseDecodedImage();
    }

    @Test
    void estimatesSourceDpiFromReferenceSizeWhenMetadataIsMissing() {
        PreprocessContext context = context(Map.of(
            "targetDpi",
            "300",
            "referenceWidthInches",
            "10.0",
            "referenceHeightInches",
            "5.0"
        ));
        ImageMatHolder sourceHolder = ImageMatHolder.decoded(
            "originals/reference-dpi.png",
            new Mat(5, 10, CvType.CV_8UC3, new Scalar(255, 255, 255))
        );
        context.storeDecodedImage(sourceHolder);

        step.execute(context);

        ImageMatHolder normalizedHolder = context.decodedImage().orElseThrow();
        assertThat(sourceHolder.released()).isTrue();
        assertThat(normalizedHolder.width()).isEqualTo(25);
        assertThat(normalizedHolder.height()).isEqualTo(13);
        assertThat(context.consumeStepNote(PreprocessStepName.DPI_NORMALIZE)).contains("sourceDpiX=1");
        context.releaseDecodedImage();
    }

    @Test
    void defersWhenDecodedImageIsMissing() {
        PreprocessContext context = context(Map.of());

        step.execute(context);

        assertThat(context.consumeStepNote(PreprocessStepName.DPI_NORMALIZE)).contains("not available");
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
