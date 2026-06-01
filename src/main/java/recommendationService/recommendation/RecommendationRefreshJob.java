package recommendationService.recommendation;

import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import recommendationService.config.RecommendationCacheProperties;

@Component
@ConditionalOnProperty(prefix = "app.features", name = "refresh-job-enabled", havingValue = "true")
public class RecommendationRefreshJob {

    private static final Logger log = LoggerFactory.getLogger(RecommendationRefreshJob.class);

    private final PodcastRecommendationCacheRepository cacheRepository;
    private final PodcastRecommendationService recommendationService;
    private final RecommendationCacheProperties cacheProperties;

    public RecommendationRefreshJob(
            PodcastRecommendationCacheRepository cacheRepository,
            PodcastRecommendationService recommendationService,
            RecommendationCacheProperties cacheProperties
    ) {
        this.cacheRepository = cacheRepository;
        this.recommendationService = recommendationService;
        this.cacheProperties = cacheProperties;
    }

    @Scheduled(fixedDelayString = "${app.jobs.recommendation-refresh-fixed-delay-ms:1200000}")
    public void refreshPersonalRecommendations() {
        Instant startedAt = Instant.now();
        int processedUsers = 0;
        int processedItems = 0;
        int errors = 0;
        for (String userId : cacheRepository.findUserIdsForRefresh(cacheProperties.refreshUserLimit())) {
            try {
                processedItems += recommendationService
                        .refreshPersonalRecommendations(userId, PodcastRecommendationScoringService.DEFAULT_LIMIT, null, true)
                        .size();
                processedUsers++;
            } catch (RuntimeException exception) {
                errors++;
                log.warn("recommendation_refresh_job_user_failed userId={}", userId, exception);
            }
        }
        log.info(
                "recommendation_refresh_job_finished durationMs={} processedUsers={} processedItems={} errors={}",
                Duration.between(startedAt, Instant.now()).toMillis(),
                processedUsers,
                processedItems,
                errors
        );
    }
}
