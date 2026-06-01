package recommendationService.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.features")
public record RecommendationFeatureFlags(
        boolean kafkaConsumersEnabled,
        boolean trendsApiEnabled,
        boolean personalPodcastsApiEnabled,
        boolean recommendationBlocksApiEnabled,
        boolean refreshJobEnabled,
        boolean globalJobEnabled,
        boolean cacheCleanupEnabled
) {
}
