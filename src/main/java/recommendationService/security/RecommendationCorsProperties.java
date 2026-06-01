package recommendationService.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cors")
public record RecommendationCorsProperties(
        List<String> allowedOrigins
) {
}
