package com.moonju.preprocess.worker.domain.preprocess.preset;

import com.moonju.preprocess.worker.domain.preprocess.step.PreprocessStepName;
import java.util.List;

public final class DocumentStepSequence {

    private DocumentStepSequence() {
    }

    public static List<PreprocessStepName> standard() {
        return List.of(
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
    }
}
