package recommendationService.recommendation;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.features", name = "global-job-enabled", havingValue = "true")
public class GlobalRecommendationsJob {

    private static final Logger log = LoggerFactory.getLogger(GlobalRecommendationsJob.class);

    private final PodcastRecommendationService recommendationService;
    private final MeterRegistry meterRegistry;

    public GlobalRecommendationsJob(PodcastRecommendationService recommendationService, MeterRegistry meterRegistry) {
        this.recommendationService = recommendationService;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(fixedDelayString = "${app.jobs.global-recommendations-fixed-delay-ms:600000}")
    public void refreshGlobalRecommendations() {
        Instant startedAt = Instant.now();
        log.info("global_recommendations_job_started");
        int processedItems = 0;
        int errors = 0;
        try {
            processedItems = recommendationService
                    .refreshGlobalPodcasts(PodcastRecommendationScoringService.MAX_LIMIT, null)
                    .size();
        } catch (RuntimeException exception) {
            errors++;
            log.warn("global_recommendations_job_failed", exception);
        }
        Duration duration = Duration.between(startedAt, Instant.now());
        Timer.builder("recommendation.jobs.duration").tag("job", "global-refresh").register(meterRegistry).record(duration);
        log.info(
                "global_recommendations_job_finished durationMs={} processedUsers={} processedItems={} errors={}",
                duration.toMillis(),
                0,
                processedItems,
                errors
        );
    }
}
