package recommendationService.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI recommendationServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Recommendation Service API")
                        .version("v1")
                        .description("Skeleton API for podcast recommendations service"));
    }
}
