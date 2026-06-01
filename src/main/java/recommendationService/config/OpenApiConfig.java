package recommendationService.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI recommendationServiceOpenApi() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(
                        "bearerJwt",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearerJwt"))
                .info(new Info()
                        .title("Recommendation Service API")
                        .version("v1")
                        .description("Read-only recommendation API backed by local Kafka-fed read models"));
    }
}
