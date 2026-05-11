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

class MorphologyCleanupStepTests {

    private final MorphologyCleanupStep step = new MorphologyCleanupStep();

    @BeforeAll
    static void loadOpenCv() {
        new OpenCvLoader().loadIfPresent();
    }

    @Test
    void appliesOpenCloseCleanupAndReleasesPreviousHolder() {
        PreprocessContext context = context(Map.of("morphologyKernelSize", "2"));
        ImageMatHolder sourceHolder = ImageMatHolder.decoded("originals/binary.png", binaryDocument());
        context.storeDecodedImage(sourceHolder);

        step.execute(context);

        ImageMatHolder cleanedHolder = context.decodedImage().orElseThrow();
        assertThat(sourceHolder.released()).isTrue();
        assertThat(cleanedHolder.loaded()).isTrue();
        assertThat(cleanedHolder.width()).isEqualTo(16);
        assertThat(cleanedHolder.height()).isEqualTo(16);
        assertThat(cleanedHolder.colorSpace()).isEqualTo("GRAY");
        assertThat(context.consumeStepNote(PreprocessStepName.MORPHOLOGY_CLEANUP)).contains("mode=open_close");
        context.releaseDecodedImage();
    }

    @Test
    void skipsWhenDisabledByParameter() {
        PreprocessContext context = context(Map.of("morphologyMode", "none"));
        ImageMatHolder sourceHolder = ImageMatHolder.decoded("originals/binary.png", binaryDocument());
        context.storeDecodedImage(sourceHolder);

        step.execute(context);

        assertThat(context.decodedImage().orElseThrow()).isSameAs(sourceHolder);
        assertThat(sourceHolder.released()).isFalse();
        assertThat(context.consumeStepNote(PreprocessStepName.MORPHOLOGY_CLEANUP)).contains("disabled");
        context.releaseDecodedImage();
    }

    @Test
    void fallsBackToOpenCloseWhenModeIsUnsupported() {
        PreprocessContext context = context(Map.of("morphologyMode", "unknown"));
        ImageMatHolder sourceHolder = ImageMatHolder.decoded("originals/binary.png", binaryDocument());
        context.storeDecodedImage(sourceHolder);

        step.execute(context);

        assertThat(context.fallbackNotes()).hasSize(1);
        assertThat(context.fallbackNotes().getFirst().stepName()).isEqualTo("MORPHOLOGY_CLEANUP");
        assertThat(context.fallbackNotes().getFirst().selectedStrategy()).isEqualTo("open_close");
        assertThat(context.consumeStepNote(PreprocessStepName.MORPHOLOGY_CLEANUP)).contains("mode=open_close");
        context.releaseDecodedImage();
    }

    @Test
    void defersWhenDecodedImageIsMissing() {
        PreprocessContext context = context(Map.of());

        step.execute(context);

        assertThat(context.consumeStepNote(PreprocessStepName.MORPHOLOGY_CLEANUP)).contains("not available");
    }

    private Mat binaryDocument() {
        Mat image = new Mat(16, 16, CvType.CV_8UC1, new Scalar(255));
        Imgproc.rectangle(image, new Point(3, 5), new Point(12, 7), new Scalar(0), -1);
        image.put(1, 1, new byte[] {0});
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
