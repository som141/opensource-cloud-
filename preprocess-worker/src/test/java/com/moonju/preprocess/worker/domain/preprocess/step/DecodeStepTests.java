package com.moonju.preprocess.worker.domain.preprocess.step;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.moonju.preprocess.worker.domain.preprocess.exception.ImageDecodeFailedException;
import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessContext;
import com.moonju.preprocess.worker.infra.opencv.ImageCodecAdapter;
import com.moonju.preprocess.worker.infra.opencv.OpenCvLoader;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class DecodeStepTests {

    private final DecodeStep decodeStep = new DecodeStep(new ImageCodecAdapter(new OpenCvLoader()));

    @Test
    void decodesSourceImageBytesIntoContextHolder() throws IOException {
        PreprocessContext context = context().withSourceImageBytes(pngBytes());

        decodeStep.execute(context);

        assertThat(context.decodedImage()).isPresent();
        assertThat(context.decodedImage().orElseThrow().loaded()).isTrue();
        assertThat(context.decodedImage().orElseThrow().width()).isEqualTo(3);
        assertThat(context.decodedImage().orElseThrow().height()).isEqualTo(2);
        assertThat(context.consumeStepNote(PreprocessStepName.DECODE))
            .contains("width=3")
            .contains("height=2");
        context.releaseDecodedImage();
    }

    @Test
    void defersDecodeWhenSourceBytesAreNotAttached() {
        PreprocessContext context = context();

        decodeStep.execute(context);

        assertThat(context.decodedImage()).isEmpty();
        assertThat(context.consumeStepNote(PreprocessStepName.DECODE))
            .contains("waiting for storage download");
    }

    @Test
    void propagatesDecodeFailureForUnsupportedBytes() {
        PreprocessContext context = context().withSourceImageBytes(new byte[] {1, 2, 3});

        assertThatThrownBy(() -> decodeStep.execute(context))
            .isInstanceOf(ImageDecodeFailedException.class)
            .hasMessageContaining("failed to decode");
        assertThat(context.decodedImage()).isEmpty();
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
