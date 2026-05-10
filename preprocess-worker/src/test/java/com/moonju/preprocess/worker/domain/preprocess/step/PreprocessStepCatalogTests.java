package com.moonju.preprocess.worker.domain.preprocess.step;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PreprocessStepCatalogTests {

    @Test
    void registersEveryRequiredDocumentStep() {
        PreprocessStepCatalog catalog = PreprocessStepCatalog.builtIn();

        assertThat(catalog.registeredNames()).containsExactlyInAnyOrder(PreprocessStepName.values());
    }
}
