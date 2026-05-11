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

class BinarizationStepTests {

    private final BinarizationStep step = new BinarizationStep();

    @BeforeAll
    static void loadOpenCv() {
        new OpenCvLoader().loadIfPresent();
    }

    @Test
    void appliesOtsuBinarizationAndReleasesPreviousHolder() {
        PreprocessContext context = context(Map.of("binarizationMode", "otsu"));
        ImageMatHolder sourceHolder = ImageMatHolder.decoded("originals/text.png", documentLikeImage());
        context.storeDecodedImage(sourceHolder);

        step.execute(context);

        ImageMatHolder binaryHolder = context.decodedImage().orElseThrow();
        assertThat(sourceHolder.released()).isTrue();
        assertThat(binaryHolder.loaded()).isTrue();
        assertThat(binaryHolder.colorSpace()).isEqualTo("GRAY");
        assertThat(binaryHolder.mat().channels()).isEqualTo(1);
        assertThat(context.consumeStepNote(PreprocessStepName.BINARIZATION)).contains("mode=otsu");
        context.releaseDecodedImage();
    }

    @Test
    void appliesAdaptiveBinarization() {
        PreprocessContext context = context(Map.of(
            "binarizationMode",
            "adaptive",
            "adaptiveBlockSize",
            "3",
            "adaptiveC",
            "5"
        ));
        ImageMatHolder sourceHolder = ImageMatHolder.decoded("originals/text.png", documentLikeImage());
        context.storeDecodedImage(sourceHolder);

        step.execute(context);

        ImageMatHolder binaryHolder = context.decodedImage().orElseThrow();
        assertThat(sourceHolder.released()).isTrue();
        assertThat(binaryHolder.colorSpace()).isEqualTo("GRAY");
        assertThat(context.consumeStepNote(PreprocessStepName.BINARIZATION)).contains("mode=adaptive");
        context.releaseDecodedImage();
    }

    @Test
    void fallsBackToOtsuWhenModeIsUnsupported() {
        PreprocessContext context = context(Map.of("binarizationMode", "unknown"));
        ImageMatHolder sourceHolder = ImageMatHolder.decoded("originals/text.png", documentLikeImage());
        context.storeDecodedImage(sourceHolder);

        step.execute(context);

        assertThat(context.fallbackNotes()).hasSize(1);
        assertThat(context.fallbackNotes().getFirst().stepName()).isEqualTo("BINARIZATION");
        assertThat(context.fallbackNotes().getFirst().selectedStrategy()).isEqualTo("otsu");
        assertThat(context.consumeStepNote(PreprocessStepName.BINARIZATION)).contains("mode=otsu");
        context.releaseDecodedImage();
    }

    @Test
    void defersWhenDecodedImageIsMissing() {
        PreprocessContext context = context(Map.of());

        step.execute(context);

        assertThat(context.consumeStepNote(PreprocessStepName.BINARIZATION)).contains("not available");
    }

    private Mat documentLikeImage() {
        Mat image = new Mat(16, 16, CvType.CV_8UC3, new Scalar(255, 255, 255));
        Imgproc.rectangle(image, new Point(3, 4), new Point(13, 6), new Scalar(0, 0, 0), -1);
        Imgproc.rectangle(image, new Point(3, 10), new Point(10, 12), new Scalar(40, 40, 40), -1);
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
