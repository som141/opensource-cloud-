package com.moonju.preprocess.worker.domain.preprocess.step;

import com.moonju.preprocess.worker.domain.preprocess.model.ImageMatHolder;
import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessContext;
import com.moonju.preprocess.worker.domain.preprocess.service.ImageDecodePort;
import org.springframework.stereotype.Component;

@Component
public class DecodeStep implements PreprocessStep {

    private final ImageDecodePort imageDecodePort;

    public DecodeStep(ImageDecodePort imageDecodePort) {
        this.imageDecodePort = imageDecodePort;
    }

    @Override
    public PreprocessStepName name() {
        return PreprocessStepName.DECODE;
    }

    @Override
    public void execute(PreprocessContext context) {
        if (!context.hasSourceImageBytes()) {
            context.recordStep(name(), "Source image bytes are not attached; decode is waiting for storage download.");
            return;
        }

        ImageMatHolder imageMatHolder = imageDecodePort.decode(context.originalObjectKey(), context.sourceImageBytes());
        context.storeDecodedImage(imageMatHolder);
        context.recordStep(
            name(),
            "Decoded image: width="
                + imageMatHolder.width()
                + ", height="
                + imageMatHolder.height()
                + ", colorSpace="
                + imageMatHolder.colorSpace()
        );
    }
}
