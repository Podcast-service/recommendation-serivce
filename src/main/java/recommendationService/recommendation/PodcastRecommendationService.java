package recommendationService.recommendation;

import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.springframework.stereotype.Service;
import recommendationService.config.RecommendationCacheProperties;

@Service
public class PodcastRecommendationService {

    private static final String CACHE_KEY_PREFIX = "podcasts:v1";
    private static final String GLOBAL_USER_ID = "__global__";

    private final PodcastRecommendationCacheRepository cacheRepository;
    private final PodcastRecommendationScoringService scoringService;
    private final RecommendationCacheProperties cacheProperties;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public PodcastRecommendationService(
            PodcastRecommendationCacheRepository cacheRepository,
            PodcastRecommendationScoringService scoringService,
            RecommendationCacheProperties cacheProperties,
            MeterRegistry meterRegistry,
            Clock clock
    ) {
        this.cacheRepository = cacheRepository;
        this.scoringService = scoringService;
        this.cacheProperties = cacheProperties;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    public List<PodcastRecommendationResponse> recommendPodcasts(
            String userId,
            Integer limit,
            String categoryId,
            Boolean excludeSeen
    ) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        int normalizedLimit = scoringService.normalizeLimit(limit);
        boolean shouldExcludeSeen = excludeSeen == null || excludeSeen;
        String requestKey = requestKey(normalizedLimit, categoryId, shouldExcludeSeen);
        Instant now = clock.instant();

        List<PodcastRecommendationResponse> cached = cacheRepository.findPersonalPodcasts(
                userId,
                requestKey,
                now,
                normalizedLimit
        );
        if (!cached.isEmpty()) {
            meterRegistry.counter("recommendation.cache.hit").increment();
            return cached;
        }

        if (!scoringService.hasPositiveProfile(userId)) {
            List<PodcastRecommendationResponse> globalCached = cacheRepository.findGlobalPodcasts(
                    requestKey(normalizedLimit, categoryId, true),
                    now,
                    normalizedLimit
            );
            if (!globalCached.isEmpty()) {
                meterRegistry.counter("recommendation.cache.hit").increment();
                return globalCached;
            }
        }

        meterRegistry.counter("recommendation.cache.miss").increment();
        return refreshPersonalRecommendations(userId, normalizedLimit, categoryId, shouldExcludeSeen);
    }

    public List<PodcastRecommendationResponse> refreshPersonalRecommendations(
            String userId,
            Integer limit,
            String categoryId,
            Boolean excludeSeen
    ) {
        int normalizedLimit = scoringService.normalizeLimit(limit);
        boolean shouldExcludeSeen = excludeSeen == null || excludeSeen;
        List<PodcastRecommendationResponse> recommendations = scoringService.calculatePodcasts(
                userId,
                normalizedLimit,
                categoryId,
                shouldExcludeSeen
        );
        Instant generatedAt = clock.instant();
        cacheRepository.replacePersonalPodcasts(
                userId,
                requestKey(normalizedLimit, categoryId, shouldExcludeSeen),
                recommendations,
                generatedAt,
                generatedAt.plus(cacheProperties.personalTtl())
        );
        meterRegistry.counter("recommendation.cache.refresh.count").increment(recommendations.size());
        return recommendations;
    }

    public List<PodcastRecommendationResponse> refreshGlobalPodcasts(Integer limit, String categoryId) {
        int normalizedLimit = scoringService.normalizeLimit(limit);
        List<PodcastRecommendationResponse> recommendations = scoringService.calculatePodcasts(
                GLOBAL_USER_ID,
                normalizedLimit,
                categoryId,
                true
        );
        Instant generatedAt = clock.instant();
        cacheRepository.replaceGlobalPodcasts(
                requestKey(normalizedLimit, categoryId, true),
                recommendations,
                generatedAt,
                generatedAt.plus(cacheProperties.globalTtl())
        );
        meterRegistry.counter("recommendation.cache.refresh.count").increment(recommendations.size());
        return recommendations;
    }

    private String requestKey(int limit, String categoryId, boolean excludeSeen) {
        return CACHE_KEY_PREFIX
                + ":limit=" + limit
                + ":category=" + encode(categoryId)
                + ":excludeSeen=" + excludeSeen;
    }

    private String encode(String value) {
        if (value == null || value.isBlank()) {
            return "all";
        }
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
