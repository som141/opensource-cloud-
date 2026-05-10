package com.moonju.preprocess.worker.domain.preprocess.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import com.moonju.preprocess.worker.domain.preprocess.preset.PreprocessPresetRegistry;
import com.moonju.preprocess.worker.domain.preprocess.step.PreprocessStep;
import com.moonju.preprocess.worker.domain.preprocess.step.PreprocessStepCatalog;
import com.moonju.preprocess.worker.domain.preprocess.step.PreprocessStepName;
import com.moonju.preprocess.worker.infra.opencv.ImageCodecAdapter;
import com.moonju.preprocess.worker.infra.opencv.OpenCvLoader;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class PreprocessPipelineRunnerTests {

    private final PreprocessPipelineRunner runner = new PreprocessPipelineRunner(
        PreprocessPresetRegistry.builtIn(),
        PreprocessStepCatalog.builtIn()
    );

    @Test
    void runsDocumentPipelineStepsInOcrPreprocessOrder() {
        PreprocessContext context = new PreprocessContext(
            1L,
            10L,
            "originals/project/scan.png",
            "LOW_CONTRAST_SCAN",
            Map.of("targetDpi", "300"),
            false
        );

        PreprocessResult result = runner.run(context);

        assertThat(result.skeletonOnly()).isTrue();
        assertThat(result.success()).isTrue();
        assertThat(result.errorMessage()).isNull();
        assertThat(result.wallTime()).isNotNull();
        assertThat(result.executedStepNames()).containsExactly(
            PreprocessStepName.DECODE,
            PreprocessStepName.COLOR_NORMALIZE,
            PreprocessStepName.ORIENTATION_NORMALIZE,
            PreprocessStepName.DESKEW,
            PreprocessStepName.CROP,
            PreprocessStepName.DENOISE,
            PreprocessStepName.CONTRAST_NORMALIZE,
            PreprocessStepName.BINARIZATION,
            PreprocessStepName.MORPHOLOGY_CLEANUP,
            PreprocessStepName.DPI_NORMALIZE,
            PreprocessStepName.OPTIONAL_SHARPEN
        );
        assertThat(result.stepExecutions())
            .allSatisfy(execution -> {
                assertThat(execution.startedAt()).isNotNull();
                assertThat(execution.endedAt()).isNotNull();
                assertThat(execution.wallTime()).isNotNull();
                assertThat(execution.success()).isTrue();
                assertThat(execution.errorMessage()).isNull();
            });
    }

    @Test
    void recordsFailedStepAndStopsPipeline() {
        PreprocessStep failingDecodeStep = new PreprocessStep() {

            @Override
            public PreprocessStepName name() {
                return PreprocessStepName.DECODE;
            }

            @Override
            public void execute(PreprocessContext context) {
                context.recordStep(name(), "Decode failed before OpenCV mat creation.");
                throw new IllegalStateException("decode failed");
            }
        };
        PreprocessPipelineRunner failingRunner = new PreprocessPipelineRunner(
            PreprocessPresetRegistry.builtIn(),
            new PreprocessStepCatalog(java.util.List.of(
                failingDecodeStep,
                new com.moonju.preprocess.worker.domain.preprocess.step.ColorNormalizeStep(),
                new com.moonju.preprocess.worker.domain.preprocess.step.OrientationNormalizeStep(),
                new com.moonju.preprocess.worker.domain.preprocess.step.DeskewStep(),
                new com.moonju.preprocess.worker.domain.preprocess.step.CropStep(),
                new com.moonju.preprocess.worker.domain.preprocess.step.DenoiseStep(),
                new com.moonju.preprocess.worker.domain.preprocess.step.ContrastNormalizeStep(),
                new com.moonju.preprocess.worker.domain.preprocess.step.BinarizationStep(),
                new com.moonju.preprocess.worker.domain.preprocess.step.MorphologyCleanupStep(),
                new com.moonju.preprocess.worker.domain.preprocess.step.DpiNormalizeStep(),
                new com.moonju.preprocess.worker.domain.preprocess.step.SharpenStep()
            ))
        );
        PreprocessContext context = new PreprocessContext(
            1L,
            10L,
            "originals/project/scan.png",
            "LOW_CONTRAST_SCAN",
            Map.of(),
            false
        );

        PreprocessResult result = failingRunner.run(context);

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("decode failed");
        assertThat(result.stepExecutions()).hasSize(1);
        assertThat(result.stepExecutions().getFirst().stepName()).isEqualTo(PreprocessStepName.DECODE);
        assertThat(result.stepExecutions().getFirst().success()).isFalse();
        assertThat(result.stepExecutions().getFirst().note()).isEqualTo("Decode failed before OpenCV mat creation.");
    }

    @Test
    void collectsFallbackNotesInContext() {
        PreprocessContext context = new PreprocessContext(
            1L,
            10L,
            "originals/project/scan.png",
            "LOW_CONTRAST_SCAN",
            Map.of(),
            false
        );

        context.recordFallback(PreprocessStepName.DESKEW, "No reliable hough lines.", "minAreaRect");

        assertThat(context.fallbackNotes()).hasSize(1);
        assertThat(context.fallbackNotes().getFirst().stepName()).isEqualTo("DESKEW");
        assertThat(context.fallbackNotes().getFirst().selectedStrategy()).isEqualTo("minAreaRect");
    }

    @Test
    void recordsDebugArtifactOnlyWhenDebugEnabled() {
        PreprocessContext debugContext = new PreprocessContext(
            3L,
            1L,
            10L,
            "originals/project/scan.png",
            "LOW_CONTRAST_SCAN",
            Map.of(),
            true
        );
        PreprocessContext normalContext = new PreprocessContext(
            3L,
            1L,
            10L,
            "originals/project/scan.png",
            "LOW_CONTRAST_SCAN",
            Map.of(),
            false
        );

        debugContext.recordDebugArtifact(PreprocessStepName.DESKEW, "02_deskew.png");
        normalContext.recordDebugArtifact(PreprocessStepName.DESKEW, "02_deskew.png");

        assertThat(debugContext.debugArtifacts()).hasSize(1);
        assertThat(debugContext.debugArtifacts().getFirst().objectKey())
            .isEqualTo("processed/3/1/10/debug/02_deskew.png");
        assertThat(debugContext.debugArtifacts().getFirst().contentType()).isEqualTo("image/png");
        assertThat(normalContext.debugArtifacts()).isEmpty();
    }

    @Test
    void releasesDecodedImageWhenPipelineFinishes() throws IOException {
        PreprocessPipelineRunner decodeRunner = new PreprocessPipelineRunner(
            PreprocessPresetRegistry.builtIn(),
            PreprocessStepCatalog.builtIn(
                new ImageCodecAdapter(new OpenCvLoader())
            )
        );
        PreprocessContext context = new PreprocessContext(
            1L,
            10L,
            "originals/project/scan.png",
            "LOW_CONTRAST_SCAN",
            Map.of(),
            false
        ).withSourceImageBytes(pngBytes());

        PreprocessResult result = decodeRunner.run(context);

        assertThat(result.success()).isTrue();
        assertThat(context.decodedImage()).isPresent();
        assertThat(context.decodedImage().orElseThrow().released()).isTrue();
        assertThat(context.decodedImage().orElseThrow().loaded()).isFalse();
        assertThat(result.stepExecutions().getFirst().note())
            .contains("width=3")
            .contains("height=2");
    }

    @Test
    void releasesDecodedImageWhenPipelineFailsAfterDecode() throws IOException {
        PreprocessStep failingColorNormalizeStep = new PreprocessStep() {

            @Override
            public PreprocessStepName name() {
                return PreprocessStepName.COLOR_NORMALIZE;
            }

            @Override
            public void execute(PreprocessContext context) {
                throw new IllegalStateException("color normalize failed");
            }
        };
        PreprocessPipelineRunner failingRunner = new PreprocessPipelineRunner(
            PreprocessPresetRegistry.builtIn(),
            new PreprocessStepCatalog(java.util.List.of(
                new com.moonju.preprocess.worker.domain.preprocess.step.DecodeStep(
                    new ImageCodecAdapter(new OpenCvLoader())
                ),
                failingColorNormalizeStep,
                new com.moonju.preprocess.worker.domain.preprocess.step.OrientationNormalizeStep(),
                new com.moonju.preprocess.worker.domain.preprocess.step.DeskewStep(),
                new com.moonju.preprocess.worker.domain.preprocess.step.CropStep(),
                new com.moonju.preprocess.worker.domain.preprocess.step.DenoiseStep(),
                new com.moonju.preprocess.worker.domain.preprocess.step.ContrastNormalizeStep(),
                new com.moonju.preprocess.worker.domain.preprocess.step.BinarizationStep(),
                new com.moonju.preprocess.worker.domain.preprocess.step.MorphologyCleanupStep(),
                new com.moonju.preprocess.worker.domain.preprocess.step.DpiNormalizeStep(),
                new com.moonju.preprocess.worker.domain.preprocess.step.SharpenStep()
            ))
        );
        PreprocessContext context = new PreprocessContext(
            1L,
            10L,
            "originals/project/scan.png",
            "LOW_CONTRAST_SCAN",
            Map.of(),
            false
        ).withSourceImageBytes(pngBytes());

        PreprocessResult result = failingRunner.run(context);

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("color normalize failed");
        assertThat(context.decodedImage()).isPresent();
        assertThat(context.decodedImage().orElseThrow().released()).isTrue();
    }

    private byte[] pngBytes() throws IOException {
        BufferedImage image = new BufferedImage(3, 2, BufferedImage.TYPE_3BYTE_BGR);
        image.setRGB(0, 0, Color.BLACK.getRGB());
        image.setRGB(1, 0, Color.WHITE.getRGB());
        image.setRGB(2, 0, Color.BLUE.getRGB());
        image.setRGB(0, 1, Color.RED.getRGB());
        image.setRGB(1, 1, Color.GREEN.getRGB());
        image.setRGB(2, 1, Color.YELLOW.getRGB());

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
