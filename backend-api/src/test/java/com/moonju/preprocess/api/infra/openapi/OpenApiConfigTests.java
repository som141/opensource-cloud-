package com.moonju.preprocess.api.infra.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

class OpenApiConfigTests {

    @Test
    void configuresJwtBearerSecurityScheme() {
        OpenAPI openAPI = new OpenApiConfig().openAPI();

        SecurityScheme securityScheme = openAPI.getComponents()
            .getSecuritySchemes()
            .get(OpenApiConfig.BEARER_AUTH);

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Image Preprocess Platform API");
        assertThat(securityScheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(securityScheme.getScheme()).isEqualTo("bearer");
        assertThat(securityScheme.getBearerFormat()).isEqualTo("JWT");
        assertThat(openAPI.getSecurity()).hasSize(1);
    }
}
