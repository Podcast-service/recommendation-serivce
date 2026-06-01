package recommendationService.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class RecommendationFeatureFlagsTest {

    @Autowired
    private RecommendationFeatureFlags featureFlags;

    @Test
    void runtimeFeaturesAreDisabledByDefault() {
        assertThat(featureFlags.kafkaConsumersEnabled()).isFalse();
        assertThat(featureFlags.refreshJobEnabled()).isFalse();
        assertThat(featureFlags.globalJobEnabled()).isFalse();
        assertThat(featureFlags.cacheCleanupEnabled()).isFalse();
    }
}
