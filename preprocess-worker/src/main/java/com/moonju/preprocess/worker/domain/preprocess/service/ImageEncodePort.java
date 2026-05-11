package com.moonju.preprocess.worker.domain.preprocess.service;

import org.opencv.core.Mat;

public interface ImageEncodePort {

    byte[] encodePng(String objectKey, Mat image);
}
