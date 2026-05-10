package com.moonju.preprocess.worker.domain.preprocess.step;

public enum PreprocessStepName {
    DECODE,
    COLOR_NORMALIZE,
    ORIENTATION_NORMALIZE,
    DESKEW,
    CROP,
    DENOISE,
    CONTRAST_NORMALIZE,
    BINARIZATION,
    MORPHOLOGY_CLEANUP,
    DPI_NORMALIZE,
    OPTIONAL_SHARPEN
}
