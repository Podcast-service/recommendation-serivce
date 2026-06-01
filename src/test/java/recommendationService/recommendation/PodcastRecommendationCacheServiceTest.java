package recommendationService.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import recommendationService.config.RecommendationCacheProperties;

class PodcastRecommendationCacheServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-02T10:15:30Z"), ZoneOffset.UTC);

    private final PodcastRecommendationCacheRepository cacheRepository = Mockito.mock(PodcastRecommendationCacheRepository.class);
    private final PodcastRecommendationScoringService scoringService = Mockito.mock(PodcastRecommendationScoringService.class);
    private final RecommendationCacheProperties cacheProperties =
            new RecommendationCacheProperties(Duration.ofMinutes(30), Duration.ofMinutes(10), 100);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final PodcastRecommendationService service = new PodcastRecommendationService(
            cacheRepository,
            scoringService,
            cacheProperties,
            meterRegistry,
            CLOCK
    );

    @Test
    void cacheHitReturnsCachedRecommendationsWithoutScoring() {
        List<PodcastRecommendationResponse> cached = List.of(response("podcast-cache", 1));
        when(scoringService.normalizeLimit(20)).thenReturn(20);
        when(cacheRepository.findPersonalPodcasts(eq("user-1"), anyString(), eq(CLOCK.instant()), eq(20), eq(true)))
                .thenReturn(cached);

        List<PodcastRecommendationResponse> response = service.recommendPodcasts("user-1", 20, null, true);

        assertThat(response).isEqualTo(cached);
        assertThat(meterRegistry.counter("recommendation.cache.hit").count()).isEqualTo(1);
        verify(scoringService, never()).calculatePodcasts(anyString(), any(), any(), any());
    }

    @Test
    void cacheMissFallsBackToOnDemandScoringAndWritesCache() {
        List<PodcastRecommendationResponse> calculated = List.of(response("podcast-new", 1));
        when(scoringService.normalizeLimit(null)).thenReturn(20);
        when(scoringService.normalizeLimit(20)).thenReturn(20);
        when(scoringService.hasPositiveProfile("user-1")).thenReturn(true);
        when(scoringService.calculatePodcasts("user-1", 20, null, true)).thenReturn(calculated);

        List<PodcastRecommendationResponse> response = service.recommendPodcasts("user-1", null, null, true);

        assertThat(response).isEqualTo(calculated);
        assertThat(meterRegistry.counter("recommendation.cache.miss").count()).isEqualTo(1);
        verify(cacheRepository).replacePersonalPodcasts(
                eq("user-1"),
                anyString(),
                eq(calculated),
                eq(CLOCK.instant()),
                eq(CLOCK.instant().plus(Duration.ofMinutes(30)))
        );
    }

    @Test
    void refreshWritesRankedEntries() {
        List<PodcastRecommendationResponse> calculated = List.of(
                response("podcast-1", 1),
                response("podcast-2", 2)
        );
        when(scoringService.normalizeLimit(20)).thenReturn(20);
        when(scoringService.calculatePodcasts("user-1", 20, "category-1", true)).thenReturn(calculated);

        List<PodcastRecommendationResponse> response = service.refreshPersonalRecommendations("user-1", 20, "category-1", true);

        assertThat(response).isEqualTo(calculated);
        assertThat(meterRegistry.counter("recommendation.cache.refresh.count").count()).isEqualTo(2);
        verify(cacheRepository).replacePersonalPodcasts(
                eq("user-1"),
                anyString(),
                eq(calculated),
                eq(CLOCK.instant()),
                eq(CLOCK.instant().plus(Duration.ofMinutes(30)))
        );
    }

    private PodcastRecommendationResponse response(String itemId, int rank) {
        return new PodcastRecommendationResponse(
                itemId,
                rank,
                new BigDecimal("80.0000"),
                "TOP_CATEGORY",
                "reason",
                Map.of("title", "Podcast")
        );
    }
}
