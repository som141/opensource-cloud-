package com.moonju.preprocess.api.infra.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Image Preprocess Platform API")
                .version("v1")
                .description("REST API for OAuth login, projects, uploads, jobs, and image preprocessing."))
            .servers(List.of(new Server().url("http://localhost:8080").description("Local backend")))
            .components(new Components().addSecuritySchemes(BEARER_AUTH, bearerSecurityScheme()))
            .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    private SecurityScheme bearerSecurityScheme() {
        return new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("Paste only the JWT access token value. Do not include the Bearer prefix.");
    }
}
