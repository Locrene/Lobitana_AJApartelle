package edu.cit.lobitana.ajapartelle.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI document for Swagger UI at /swagger-ui.html (NFR-18). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ajApartelleOpenApi() {
        return new OpenAPI().info(new Info()
                .title("AJ Apartelle API")
                .version("v1")
                .description("Booking and operations REST API. Signed-in calls use the httpOnly AJ_TOKEN cookie "
                        + "set by POST /api/v1/auth/login; sign in first, then try the protected endpoints."));
    }
}
