package com.moonju.preprocess.api.domain.image.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.moonju.preprocess.api.domain.image.model.ImageMetadata;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ImageMetadataExtractorTests {

    private final ImageMetadataExtractor extractor = new ImageMetadataExtractor();

    @Test
    void extractsPngDimensionsAndDpi() {
        ImageMetadata metadata = extractor.extract(pngBytes(1240, 1754, 300));

        assertThat(metadata.width()).isEqualTo(1240);
        assertThat(metadata.height()).isEqualTo(1754);
        assertThat(metadata.dpiX()).isEqualTo(300);
        assertThat(metadata.dpiY()).isEqualTo(300);
    }

    @Test
    void extractsJpegDimensionsAndDpi() {
        ImageMetadata metadata = extractor.extract(jpegBytes(1240, 1754, 300));

        assertThat(metadata.width()).isEqualTo(1240);
        assertThat(metadata.height()).isEqualTo(1754);
        assertThat(metadata.dpiX()).isEqualTo(300);
        assertThat(metadata.dpiY()).isEqualTo(300);
    }

    @Test
    void extractsWebpVp8xDimensions() {
        ImageMetadata metadata = extractor.extract(webpVp8xBytes(640, 480));

        assertThat(metadata.width()).isEqualTo(640);
        assertThat(metadata.height()).isEqualTo(480);
        assertThat(metadata.dpiX()).isNull();
        assertThat(metadata.dpiY()).isNull();
    }

    @Test
    void extractsBmpDimensions() {
        ImageMetadata metadata = extractor.extract(bmpBytes(1024, 768));

        assertThat(metadata.width()).isEqualTo(1024);
        assertThat(metadata.height()).isEqualTo(768);
        assertThat(metadata.dpiX()).isNull();
        assertThat(metadata.dpiY()).isNull();
    }

    @Test
    void returnsEmptyMetadataForUnsupportedBytes() {
        ImageMetadata metadata = extractor.extract("not-an-image".getBytes(StandardCharsets.UTF_8));

        assertThat(metadata.width()).isNull();
        assertThat(metadata.height()).isNull();
        assertThat(metadata.dpiX()).isNull();
        assertThat(metadata.dpiY()).isNull();
    }

    private byte[] pngBytes(int width, int height, int dpi) {
        byte[] bytes = new byte[54];
        byte[] signature = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        };
        System.arraycopy(signature, 0, bytes, 0, signature.length);
        putIntBigEndian(bytes, 8, 13);
        putAscii(bytes, 12, "IHDR");
        putIntBigEndian(bytes, 16, width);
        putIntBigEndian(bytes, 20, height);
        bytes[24] = 8;
        bytes[25] = 2;

        putIntBigEndian(bytes, 33, 9);
        putAscii(bytes, 37, "pHYs");
        int pixelsPerMeter = Math.round(dpi / 0.0254F);
        putIntBigEndian(bytes, 41, pixelsPerMeter);
        putIntBigEndian(bytes, 45, pixelsPerMeter);
        bytes[49] = 1;
        return bytes;
    }

    private byte[] jpegBytes(int width, int height, int dpi) {
        byte[] bytes = new byte[41];
        bytes[0] = (byte) 0xFF;
        bytes[1] = (byte) 0xD8;

        bytes[2] = (byte) 0xFF;
        bytes[3] = (byte) 0xE0;
        putShortBigEndian(bytes, 4, 16);
        putAscii(bytes, 6, "JFIF");
        bytes[10] = 0;
        bytes[11] = 1;
        bytes[12] = 1;
        bytes[13] = 1;
        putShortBigEndian(bytes, 14, dpi);
        putShortBigEndian(bytes, 16, dpi);

        bytes[20] = (byte) 0xFF;
        bytes[21] = (byte) 0xC0;
        putShortBigEndian(bytes, 22, 17);
        bytes[24] = 8;
        putShortBigEndian(bytes, 25, height);
        putShortBigEndian(bytes, 27, width);
        bytes[29] = 3;
        return bytes;
    }

    private byte[] webpVp8xBytes(int width, int height) {
        byte[] bytes = new byte[30];
        putAscii(bytes, 0, "RIFF");
        putAscii(bytes, 8, "WEBP");
        putAscii(bytes, 12, "VP8X");
        putIntLittleEndian(bytes, 16, 10);
        putUnsigned24LittleEndian(bytes, 24, width - 1);
        putUnsigned24LittleEndian(bytes, 27, height - 1);
        return bytes;
    }

    private byte[] bmpBytes(int width, int height) {
        byte[] bytes = new byte[26];
        bytes[0] = 0x42;
        bytes[1] = 0x4D;
        putIntLittleEndian(bytes, 18, width);
        putIntLittleEndian(bytes, 22, height);
        return bytes;
    }

    private void putAscii(byte[] bytes, int offset, String value) {
        byte[] ascii = value.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(ascii, 0, bytes, offset, ascii.length);
    }

    private void putShortBigEndian(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) ((value >>> 8) & 0xFF);
        bytes[offset + 1] = (byte) (value & 0xFF);
    }

    private void putIntBigEndian(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) ((value >>> 24) & 0xFF);
        bytes[offset + 1] = (byte) ((value >>> 16) & 0xFF);
        bytes[offset + 2] = (byte) ((value >>> 8) & 0xFF);
        bytes[offset + 3] = (byte) (value & 0xFF);
    }

    private void putIntLittleEndian(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) (value & 0xFF);
        bytes[offset + 1] = (byte) ((value >>> 8) & 0xFF);
        bytes[offset + 2] = (byte) ((value >>> 16) & 0xFF);
        bytes[offset + 3] = (byte) ((value >>> 24) & 0xFF);
    }

    private void putUnsigned24LittleEndian(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) (value & 0xFF);
        bytes[offset + 1] = (byte) ((value >>> 8) & 0xFF);
        bytes[offset + 2] = (byte) ((value >>> 16) & 0xFF);
    }
}
