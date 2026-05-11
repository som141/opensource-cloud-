package com.moonju.preprocess.worker.infra.opencv;

import com.moonju.preprocess.worker.domain.preprocess.exception.ImageDecodeFailedException;
import com.moonju.preprocess.worker.domain.preprocess.model.ImageMatHolder;
import com.moonju.preprocess.worker.domain.preprocess.service.ImageDecodePort;
import com.moonju.preprocess.worker.domain.preprocess.service.ImageEncodePort;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.stereotype.Component;

@Component
public class ImageCodecAdapter implements ImageDecodePort, ImageEncodePort {

    private final OpenCvLoader openCvLoader;

    public ImageCodecAdapter(OpenCvLoader openCvLoader) {
        this.openCvLoader = openCvLoader;
    }

    @Override
    public ImageMatHolder decode(String objectKey, byte[] imageBytes) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new ImageDecodeFailedException("Source object key is required for image decode.");
        }
        if (imageBytes == null || imageBytes.length == 0) {
            throw new ImageDecodeFailedException("Image bytes are empty: " + objectKey);
        }

        openCvLoader.load();
        MatOfByte encoded = new MatOfByte(imageBytes);
        Mat decoded = Imgcodecs.imdecode(encoded, Imgcodecs.IMREAD_UNCHANGED);
        encoded.release();

        if (decoded == null || decoded.empty()) {
            if (decoded != null) {
                decoded.release();
            }
            throw new ImageDecodeFailedException("OpenCV failed to decode image bytes: " + objectKey);
        }

        return ImageMatHolder.decoded(objectKey, decoded);
    }

    public ImageMatHolder decodePlaceholder(String objectKey) {
        return ImageMatHolder.placeholder(objectKey);
    }

    @Override
    public byte[] encodePng(String objectKey, Mat image) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new ImageDecodeFailedException("Target object key is required for image encode.");
        }
        if (image == null || image.empty()) {
            throw new ImageDecodeFailedException("Image Mat is empty for PNG encode: " + objectKey);
        }

        openCvLoader.load();
        MatOfByte encoded = new MatOfByte();
        boolean encodedSuccessfully = Imgcodecs.imencode(".png", image, encoded);
        try {
            if (!encodedSuccessfully || encoded.empty()) {
                throw new ImageDecodeFailedException("OpenCV failed to encode PNG: " + objectKey);
            }
            return encoded.toArray();
        } finally {
            encoded.release();
        }
    }
}
