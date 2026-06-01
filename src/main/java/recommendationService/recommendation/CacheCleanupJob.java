package recommendationService.recommendation;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.features", name = "cache-cleanup-enabled", havingValue = "true")
public class CacheCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(CacheCleanupJob.class);

    private final PodcastRecommendationCacheRepository cacheRepository;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public CacheCleanupJob(
            PodcastRecommendationCacheRepository cacheRepository,
            MeterRegistry meterRegistry,
            Clock clock
    ) {
        this.cacheRepository = cacheRepository;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.jobs.cache-cleanup-fixed-delay-ms:3600000}")
    public void cleanupExpiredCache() {
        Instant startedAt = Instant.now();
        int deleted = 0;
        int errors = 0;
        try {
            deleted = cacheRepository.cleanupExpired(clock.instant());
            meterRegistry.counter("recommendation.cache.cleanup.count").increment(deleted);
        } catch (RuntimeException exception) {
            errors++;
            log.warn("recommendation_cache_cleanup_job_failed", exception);
        }
        log.info(
                "recommendation_cache_cleanup_job_finished durationMs={} processedUsers={} processedItems={} errors={}",
                Duration.between(startedAt, Instant.now()).toMillis(),
                0,
                deleted,
                errors
        );
    }
}
