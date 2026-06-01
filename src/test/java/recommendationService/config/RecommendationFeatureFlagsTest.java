package recommendationService.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import recommendationService.kafka.PodcastActivityEventConsumer;
import recommendationService.kafka.PodcastContentEventConsumer;

@SpringBootTest
@ActiveProfiles("test")
class RecommendationFeatureFlagsTest {

    @Autowired
    private RecommendationFeatureFlags featureFlags;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void runtimeFeaturesAreDisabledByDefault() {
        assertThat(featureFlags.kafkaConsumersEnabled()).isFalse();
        assertThat(featureFlags.refreshJobEnabled()).isFalse();
        assertThat(featureFlags.globalJobEnabled()).isFalse();
        assertThat(featureFlags.cacheCleanupEnabled()).isFalse();
    }

    @Test
    void kafkaConsumerBeanIsDisabledByDefault() {
        assertThat(applicationContext.getBeansOfType(PodcastContentEventConsumer.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(PodcastActivityEventConsumer.class)).isEmpty();
    }
}
