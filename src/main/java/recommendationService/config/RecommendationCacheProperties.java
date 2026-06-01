package recommendationService.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.recommendation-cache")
public record RecommendationCacheProperties(
        Duration personalTtl,
        Duration globalTtl,
        int refreshUserLimit
) {
}
