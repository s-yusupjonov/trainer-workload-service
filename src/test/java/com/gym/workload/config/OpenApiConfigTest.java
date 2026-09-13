package com.gym.workload.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig();

    @Test
    void trainerWorkloadOpenApi_configuresInfoAndBearerAuth() {
        OpenAPI openApi = config.trainerWorkloadOpenApi();

        assertThat(openApi.getInfo().getTitle()).isEqualTo("Trainer Workload Service API");
        assertThat(openApi.getInfo().getVersion()).isEqualTo("v1");
        assertThat(openApi.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
        assertThat(openApi.getSecurity()).isNotEmpty();
    }
}
