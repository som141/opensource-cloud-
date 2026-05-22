package com.moonju.preprocess.api.domain.image.service;

import com.moonju.preprocess.api.domain.image.model.ImageMetadata;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.springframework.stereotype.Component;

@Component
public class ImageMetadataExtractor {

    private static final byte[] PNG_SIGNATURE = {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    public ImageMetadata extract(byte[] bytes) {
        try {
            if (startsWith(bytes, PNG_SIGNATURE)) {
                return extractPng(bytes);
            }
            if (isJpeg(bytes)) {
                return extractJpeg(bytes);
            }
            if (isWebp(bytes)) {
                return extractWebp(bytes);
            }
            if (isBmp(bytes)) {
                return extractBmp(bytes);
            }
            if (isTiff(bytes)) {
                return extractTiff(bytes);
            }
        } catch (RuntimeException exception) {
            return ImageMetadata.empty();
        }
        return ImageMetadata.empty();
    }

    private ImageMetadata extractPng(byte[] bytes) {
        Integer width = null;
        Integer height = null;
        Integer dpiX = null;
        Integer dpiY = null;
        int offset = 8;
        while (offset + 12 <= bytes.length) {
            int length = readIntBigEndian(bytes, offset);
            String type = ascii(bytes, offset + 4, 4);
            int dataOffset = offset + 8;
            if (dataOffset + length > bytes.length) {
                break;
            }
            if ("IHDR".equals(type) && length >= 8) {
                width = readIntBigEndian(bytes, dataOffset);
                height = readIntBigEndian(bytes, dataOffset + 4);
            }
            if ("pHYs".equals(type) && length >= 9 && bytes[dataOffset + 8] == 1) {
                dpiX = pixelsPerMeterToDpi(readIntBigEndian(bytes, dataOffset));
                dpiY = pixelsPerMeterToDpi(readIntBigEndian(bytes, dataOffset + 4));
            }
            offset = dataOffset + length + 4;
        }
        return new ImageMetadata(width, height, dpiX, dpiY);
    }

    private ImageMetadata extractJpeg(byte[] bytes) {
        Integer width = null;
        Integer height = null;
        Integer dpiX = null;
        Integer dpiY = null;
        int offset = 2;
        while (offset + 4 <= bytes.length) {
            if (unsigned(bytes[offset]) != 0xFF) {
                offset++;
                continue;
            }
            int marker = unsigned(bytes[offset + 1]);
            offset += 2;
            if (marker == 0xD9 || marker == 0xDA) {
                break;
            }
            if (offset + 2 > bytes.length) {
                break;
            }
            int length = readUnsignedShortBigEndian(bytes, offset);
            if (length < 2 || offset + length > bytes.length) {
                break;
            }
            int dataOffset = offset + 2;
            if (marker == 0xE0 && length >= 16 && asciiEquals(bytes, dataOffset, "JFIF")) {
                int unit = unsigned(bytes[dataOffset + 7]);
                int xDensity = readUnsignedShortBigEndian(bytes, dataOffset + 8);
                int yDensity = readUnsignedShortBigEndian(bytes, dataOffset + 10);
                if (unit == 1) {
                    dpiX = xDensity;
                    dpiY = yDensity;
                } else if (unit == 2) {
                    dpiX = dotsPerCentimeterToDpi(xDensity);
                    dpiY = dotsPerCentimeterToDpi(yDensity);
                }
            }
            if (isStartOfFrame(marker) && length >= 7) {
                height = readUnsignedShortBigEndian(bytes, dataOffset + 1);
                width = readUnsignedShortBigEndian(bytes, dataOffset + 3);
            }
            offset += length;
        }
        return new ImageMetadata(width, height, dpiX, dpiY);
    }

    private ImageMetadata extractWebp(byte[] bytes) {
        String chunk = ascii(bytes, 12, 4);
        if ("VP8X".equals(chunk) && bytes.length >= 30) {
            int width = 1 + readUnsigned24LittleEndian(bytes, 24);
            int height = 1 + readUnsigned24LittleEndian(bytes, 27);
            return new ImageMetadata(width, height, null, null);
        }
        if ("VP8L".equals(chunk) && bytes.length >= 25 && unsigned(bytes[20]) == 0x2F) {
            int b1 = unsigned(bytes[21]);
            int b2 = unsigned(bytes[22]);
            int b3 = unsigned(bytes[23]);
            int b4 = unsigned(bytes[24]);
            int width = 1 + (((b2 & 0x3F) << 8) | b1);
            int height = 1 + (((b4 & 0x0F) << 10) | (b3 << 2) | ((b2 & 0xC0) >> 6));
            return new ImageMetadata(width, height, null, null);
        }
        if ("VP8 ".equals(chunk) && bytes.length >= 30 && unsigned(bytes[23]) == 0x9D
            && unsigned(bytes[24]) == 0x01 && unsigned(bytes[25]) == 0x2A) {
            int width = readUnsignedShortLittleEndian(bytes, 26) & 0x3FFF;
            int height = readUnsignedShortLittleEndian(bytes, 28) & 0x3FFF;
            return new ImageMetadata(width, height, null, null);
        }
        return ImageMetadata.empty();
    }

    private ImageMetadata extractBmp(byte[] bytes) {
        if (bytes.length < 26) {
            return ImageMetadata.empty();
        }
        int width = readIntLittleEndian(bytes, 18);
        int height = Math.abs(readIntLittleEndian(bytes, 22));
        return new ImageMetadata(width, height, null, null);
    }

    private ImageMetadata extractTiff(byte[] bytes) {
        ByteOrder byteOrder = tiffByteOrder(bytes);
        long ifdOffset = readUnsignedInt(bytes, 4, byteOrder);
        if (ifdOffset < 0 || ifdOffset + 2 > bytes.length) {
            return ImageMetadata.empty();
        }
        int entryCount = readUnsignedShort(bytes, (int) ifdOffset, byteOrder);
        int offset = (int) ifdOffset + 2;
        Integer width = null;
        Integer height = null;
        Long xResolutionOffset = null;
        Long yResolutionOffset = null;
        Integer resolutionUnit = 2;

        for (int index = 0; index < entryCount && offset + 12 <= bytes.length; index++) {
            int tag = readUnsignedShort(bytes, offset, byteOrder);
            int type = readUnsignedShort(bytes, offset + 2, byteOrder);
            long count = readUnsignedInt(bytes, offset + 4, byteOrder);
            int valueOffset = offset + 8;
            if (count == 1 && tag == 256) {
                width = readTiffScalar(bytes, valueOffset, type, byteOrder);
            } else if (count == 1 && tag == 257) {
                height = readTiffScalar(bytes, valueOffset, type, byteOrder);
            } else if (count == 1 && tag == 282) {
                xResolutionOffset = readUnsignedInt(bytes, valueOffset, byteOrder);
            } else if (count == 1 && tag == 283) {
                yResolutionOffset = readUnsignedInt(bytes, valueOffset, byteOrder);
            } else if (count == 1 && tag == 296) {
                resolutionUnit = readTiffScalar(bytes, valueOffset, type, byteOrder);
            }
            offset += 12;
        }

        Integer dpiX = readTiffDpi(bytes, xResolutionOffset, resolutionUnit, byteOrder);
        Integer dpiY = readTiffDpi(bytes, yResolutionOffset, resolutionUnit, byteOrder);
        return new ImageMetadata(width, height, dpiX, dpiY);
    }

    private Integer readTiffScalar(byte[] bytes, int offset, int type, ByteOrder byteOrder) {
        if (type == 3) {
            return readUnsignedShort(bytes, offset, byteOrder);
        }
        if (type == 4) {
            return Math.toIntExact(readUnsignedInt(bytes, offset, byteOrder));
        }
        return null;
    }

    private Integer readTiffDpi(byte[] bytes, Long offset, Integer resolutionUnit, ByteOrder byteOrder) {
        if (offset == null || offset + 8 > bytes.length) {
            return null;
        }
        long numerator = readUnsignedInt(bytes, offset.intValue(), byteOrder);
        long denominator = readUnsignedInt(bytes, offset.intValue() + 4, byteOrder);
        if (denominator == 0) {
            return null;
        }
        double value = (double) numerator / denominator;
        if (resolutionUnit != null && resolutionUnit == 3) {
            value *= 2.54;
        }
        return (int) Math.round(value);
    }

    private boolean isStartOfFrame(int marker) {
        return marker == 0xC0 || marker == 0xC1 || marker == 0xC2 || marker == 0xC3
            || marker == 0xC5 || marker == 0xC6 || marker == 0xC7
            || marker == 0xC9 || marker == 0xCA || marker == 0xCB
            || marker == 0xCD || marker == 0xCE || marker == 0xCF;
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3
            && unsigned(bytes[0]) == 0xFF
            && unsigned(bytes[1]) == 0xD8
            && unsigned(bytes[2]) == 0xFF;
    }

    private boolean isWebp(byte[] bytes) {
        return bytes.length >= 12 && asciiEquals(bytes, 0, "RIFF") && asciiEquals(bytes, 8, "WEBP");
    }

    private boolean isBmp(byte[] bytes) {
        return bytes.length >= 2 && bytes[0] == 0x42 && bytes[1] == 0x4D;
    }

    private boolean isTiff(byte[] bytes) {
        return bytes.length >= 4
            && ((bytes[0] == 0x49 && bytes[1] == 0x49 && bytes[2] == 0x2A && bytes[3] == 0x00)
            || (bytes[0] == 0x4D && bytes[1] == 0x4D && bytes[2] == 0x00 && bytes[3] == 0x2A));
    }

    private ByteOrder tiffByteOrder(byte[] bytes) {
        if (bytes[0] == 0x49 && bytes[1] == 0x49) {
            return ByteOrder.LITTLE_ENDIAN;
        }
        return ByteOrder.BIG_ENDIAN;
    }

    private boolean startsWith(byte[] bytes, byte[] signature) {
        if (bytes.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (bytes[index] != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean asciiEquals(byte[] bytes, int offset, String expected) {
        if (bytes.length < offset + expected.length()) {
            return false;
        }
        for (int index = 0; index < expected.length(); index++) {
            if (bytes[offset + index] != (byte) expected.charAt(index)) {
                return false;
            }
        }
        return true;
    }

    private String ascii(byte[] bytes, int offset, int length) {
        if (bytes.length < offset + length) {
            return "";
        }
        return new String(bytes, offset, length, java.nio.charset.StandardCharsets.US_ASCII);
    }

    private int readIntBigEndian(byte[] bytes, int offset) {
        return ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    private int readIntLittleEndian(byte[] bytes, int offset) {
        return ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private int readUnsignedShortBigEndian(byte[] bytes, int offset) {
        return readUnsignedShort(bytes, offset, ByteOrder.BIG_ENDIAN);
    }

    private int readUnsignedShortLittleEndian(byte[] bytes, int offset) {
        return readUnsignedShort(bytes, offset, ByteOrder.LITTLE_ENDIAN);
    }

    private int readUnsignedShort(byte[] bytes, int offset, ByteOrder byteOrder) {
        return Short.toUnsignedInt(ByteBuffer.wrap(bytes, offset, 2).order(byteOrder).getShort());
    }

    private long readUnsignedInt(byte[] bytes, int offset, ByteOrder byteOrder) {
        return Integer.toUnsignedLong(ByteBuffer.wrap(bytes, offset, 4).order(byteOrder).getInt());
    }

    private int readUnsigned24LittleEndian(byte[] bytes, int offset) {
        return unsigned(bytes[offset]) | (unsigned(bytes[offset + 1]) << 8) | (unsigned(bytes[offset + 2]) << 16);
    }

    private int pixelsPerMeterToDpi(int pixelsPerMeter) {
        return (int) Math.round(pixelsPerMeter * 0.0254);
    }

    private int dotsPerCentimeterToDpi(int dotsPerCentimeter) {
        return (int) Math.round(dotsPerCentimeter * 2.54);
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }
}
