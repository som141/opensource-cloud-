package com.moonju.preprocess.worker.infra.opencv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.moonju.preprocess.worker.domain.preprocess.exception.ImageDecodeFailedException;
import com.moonju.preprocess.worker.domain.preprocess.model.ImageMatHolder;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ImageCodecAdapterTests {

    @Test
    void decodePngBytesReturnsLoadedImageMatHolder() throws IOException {
        ImageCodecAdapter adapter = new ImageCodecAdapter(new OpenCvLoader());

        ImageMatHolder holder = adapter.decode("originals/scan.png", pngBytes());

        assertThat(holder.sourceObjectKey()).isEqualTo("originals/scan.png");
        assertThat(holder.placeholder()).isFalse();
        assertThat(holder.loaded()).isTrue();
        assertThat(holder.width()).isEqualTo(3);
        assertThat(holder.height()).isEqualTo(2);
        assertThat(holder.colorSpace()).isIn("BGR", "BGRA");
        holder.release();
        assertThat(holder.released()).isTrue();
    }

    @Test
    void decodeRejectsEmptyBytes() {
        ImageCodecAdapter adapter = new ImageCodecAdapter(new OpenCvLoader());

        assertThatThrownBy(() -> adapter.decode("originals/empty.png", new byte[0]))
            .isInstanceOf(ImageDecodeFailedException.class)
            .hasMessageContaining("empty");
    }

    @Test
    void decodeRejectsUnsupportedBytes() {
        ImageCodecAdapter adapter = new ImageCodecAdapter(new OpenCvLoader());

        assertThatThrownBy(() -> adapter.decode("originals/not-image.bin", new byte[] {1, 2, 3}))
            .isInstanceOf(ImageDecodeFailedException.class)
            .hasMessageContaining("failed to decode");
    }

    @Test
    void matResourceCleanerReleasesHolder() throws IOException {
        ImageCodecAdapter adapter = new ImageCodecAdapter(new OpenCvLoader());
        MatResourceCleaner cleaner = new MatResourceCleaner();
        ImageMatHolder holder = adapter.decode("originals/scan.png", pngBytes());

        cleaner.release(holder);

        assertThat(holder.released()).isTrue();
        assertThat(holder.loaded()).isFalse();
    }

    @Test
    void loadIfPresentIsIdempotent() {
        OpenCvLoader loader = new OpenCvLoader();

        assertThat(loader.loadIfPresent()).isTrue();
        assertThat(loader.loadIfPresent()).isTrue();
        assertThat(loader.loaded()).isTrue();
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
