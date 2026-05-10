package com.moonju.preprocess.worker.infra.opencv;

import static org.assertj.core.api.Assertions.assertThat;

import com.moonju.preprocess.worker.domain.preprocess.model.ImageMatHolder;
import org.junit.jupiter.api.Test;

class ImageCodecAdapterTests {

    @Test
    void decodePlaceholderReturnsUnloadedImageMatHolder() {
        ImageCodecAdapter adapter = new ImageCodecAdapter(new OpenCvLoader());

        ImageMatHolder holder = adapter.decodePlaceholder("originals/scan.png");

        assertThat(holder.sourceObjectKey()).isEqualTo("originals/scan.png");
        assertThat(holder.placeholder()).isTrue();
        assertThat(holder.loaded()).isFalse();
        assertThat(holder.colorSpace()).isEqualTo("UNLOADED");
    }
}
