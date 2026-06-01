package recommendationService.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import recommendationService.recommendation.CacheCleanupJob;
import recommendationService.recommendation.GlobalRecommendationsJob;
import recommendationService.recommendation.RecommendationRefreshJob;
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
    void featureFlagsUseSafeDefaults() {
        assertThat(featureFlags.kafkaConsumersEnabled()).isFalse();
        assertThat(featureFlags.trendsApiEnabled()).isTrue();
        assertThat(featureFlags.personalPodcastsApiEnabled()).isTrue();
        assertThat(featureFlags.recommendationBlocksApiEnabled()).isTrue();
        assertThat(featureFlags.refreshJobEnabled()).isFalse();
        assertThat(featureFlags.globalJobEnabled()).isFalse();
        assertThat(featureFlags.cacheCleanupEnabled()).isFalse();
    }

    @Test
    void kafkaConsumerBeanIsDisabledByDefault() {
        assertThat(applicationContext.getBeansOfType(PodcastContentEventConsumer.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(PodcastActivityEventConsumer.class)).isEmpty();
    }

    @Test
    void scheduledJobsAreDisabledByDefault() {
        assertThat(applicationContext.getBeansOfType(RecommendationRefreshJob.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(GlobalRecommendationsJob.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(CacheCleanupJob.class)).isEmpty();
    }
}
