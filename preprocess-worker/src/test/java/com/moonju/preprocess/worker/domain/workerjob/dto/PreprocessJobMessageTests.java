package com.moonju.preprocess.worker.domain.workerjob.dto;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.moonju.preprocess.worker.domain.workerjob.exception.InvalidWorkerMessageException;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PreprocessJobMessageTests {

    @Test
    void validatesRequiredIdentityFields() {
        PreprocessJobMessage message = new PreprocessJobMessage(
            null,
            1L,
            2L,
            3L,
            4L,
            5L,
            "originals/scan.png",
            "A4_SCAN_300DPI",
            Map.of(),
            false,
            JobPriority.NORMAL,
            1,
            "trace",
            Instant.parse("2026-05-10T00:00:00Z")
        );

        assertThatThrownBy(message::validateRequiredFields)
            .isInstanceOf(InvalidWorkerMessageException.class)
            .hasMessage("Message identity fields are required.");
    }

    @Test
    void validatesAttempt() {
        PreprocessJobMessage message = validMessage(0);

        assertThatThrownBy(message::validateRequiredFields)
            .isInstanceOf(InvalidWorkerMessageException.class)
            .hasMessage("Attempt must be greater than zero.");
    }

    private PreprocessJobMessage validMessage(int attempt) {
        return new PreprocessJobMessage(
            "msg",
            1L,
            2L,
            3L,
            4L,
            5L,
            "originals/scan.png",
            "A4_SCAN_300DPI",
            Map.of(),
            false,
            JobPriority.NORMAL,
            attempt,
            "trace",
            Instant.parse("2026-05-10T00:00:00Z")
        );
    }
}
