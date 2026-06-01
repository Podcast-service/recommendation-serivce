package recommendationService.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PodcastRecommendationServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-02T10:15:30Z"), ZoneOffset.UTC);

    private final PodcastRecommendationQueryRepository repository = Mockito.mock(PodcastRecommendationQueryRepository.class);
    private final PodcastRecommendationService service = new PodcastRecommendationService(repository, CLOCK);

    @Test
    void userWithCategoryInterestGetsRelevantPodcasts() {
        when(repository.findTopCategoryInterests("user-1", 20))
                .thenReturn(List.of(new InterestScore("category-1", new BigDecimal("10"))));
        when(repository.findTopAuthorInterests("user-1", 20)).thenReturn(List.of());
        when(repository.findCandidates(
                "user-1",
                List.of("category-1"),
                List.of(),
                null,
                LocalDate.parse("2026-05-27"),
                LocalDate.parse("2026-06-02"),
                Instant.parse("2026-05-03T10:15:30Z"),
                500
        )).thenReturn(List.of(
                candidate("podcast-category", "author-2", "category-1", "15", false, false, 0),
                candidate("podcast-other", "author-3", "category-2", "20", false, false, 0)
        ));

        List<PodcastRecommendationResponse> response = service.recommendPodcasts("user-1", 20, null, true);

        assertThat(response).isNotEmpty();
        assertThat(response.getFirst().itemId()).isEqualTo("podcast-category");
        assertThat(response.getFirst().reasonCode()).isEqualTo("TOP_CATEGORY");
    }

    @Test
    void userWithAuthorInterestGetsPodcastsFromAuthor() {
        when(repository.findTopCategoryInterests("user-1", 20)).thenReturn(List.of());
        when(repository.findTopAuthorInterests("user-1", 20))
                .thenReturn(List.of(new InterestScore("author-1", new BigDecimal("5"))));
        when(repository.findCandidates(
                "user-1",
                List.of(),
                List.of("author-1"),
                null,
                LocalDate.parse("2026-05-27"),
                LocalDate.parse("2026-06-02"),
                Instant.parse("2026-05-03T10:15:30Z"),
                500
        )).thenReturn(List.of(
                candidate("podcast-author", "author-1", "category-2", "5", false, false, 0),
                candidate("podcast-other", "author-2", "category-3", "10", false, false, 0)
        ));

        List<PodcastRecommendationResponse> response = service.recommendPodcasts("user-1", null, null, true);

        assertThat(response.getFirst().itemId()).isEqualTo("podcast-author");
        assertThat(response.getFirst().reasonCode()).isEqualTo("TOP_AUTHOR");
    }

    @Test
    void dislikedPodcastIsNotReturned() {
        when(repository.findTopCategoryInterests("user-1", 20)).thenReturn(List.of());
        when(repository.findTopAuthorInterests("user-1", 20)).thenReturn(List.of());
        when(repository.findCandidates(
                "user-1",
                List.of(),
                List.of(),
                null,
                LocalDate.parse("2026-05-27"),
                LocalDate.parse("2026-06-02"),
                Instant.parse("2026-05-03T10:15:30Z"),
                500
        )).thenReturn(List.of(
                candidate("podcast-disliked", "author-1", "category-1", "100", false, true, 0),
                candidate("podcast-allowed", "author-2", "category-2", "10", false, false, 0)
        ));

        List<PodcastRecommendationResponse> response = service.recommendPodcasts("user-1", null, null, true);

        assertThat(response).extracting(PodcastRecommendationResponse::itemId)
                .containsExactly("podcast-allowed");
    }

    @Test
    void excludeSeenSkipsAlreadyInteractedPodcasts() {
        when(repository.findTopCategoryInterests("user-1", 20)).thenReturn(List.of());
        when(repository.findTopAuthorInterests("user-1", 20)).thenReturn(List.of());
        when(repository.findCandidates(
                "user-1",
                List.of(),
                List.of(),
                null,
                LocalDate.parse("2026-05-27"),
                LocalDate.parse("2026-06-02"),
                Instant.parse("2026-05-03T10:15:30Z"),
                500
        )).thenReturn(List.of(
                candidate("podcast-seen", "author-1", "category-1", "100", false, false, 1),
                candidate("podcast-new", "author-2", "category-2", "10", false, false, 0)
        ));

        List<PodcastRecommendationResponse> response = service.recommendPodcasts("user-1", null, null, true);

        assertThat(response).extracting(PodcastRecommendationResponse::itemId)
                .containsExactly("podcast-new");
    }

    @Test
    void emptyProfileFallsBackToGlobalPopular() {
        when(repository.findTopCategoryInterests("user-1", 20)).thenReturn(List.of());
        when(repository.findTopAuthorInterests("user-1", 20)).thenReturn(List.of());
        when(repository.findCandidates(
                "user-1",
                List.of(),
                List.of(),
                null,
                LocalDate.parse("2026-05-27"),
                LocalDate.parse("2026-06-02"),
                Instant.parse("2026-05-03T10:15:30Z"),
                500
        )).thenReturn(List.of(candidate("podcast-popular", "author-1", "category-1", "50", false, false, 0)));

        List<PodcastRecommendationResponse> response = service.recommendPodcasts("user-1", null, null, true);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().reasonCode()).isEqualTo("FALLBACK_POPULAR");
    }

    @Test
    void validatesLimitAndDelegatesCategoryFilter() {
        when(repository.findTopCategoryInterests("user-1", 20)).thenReturn(List.of());
        when(repository.findTopAuthorInterests("user-1", 20)).thenReturn(List.of());
        when(repository.findCandidates(
                "user-1",
                List.of(),
                List.of(),
                "category-1",
                LocalDate.parse("2026-05-27"),
                LocalDate.parse("2026-06-02"),
                Instant.parse("2026-05-03T10:15:30Z"),
                500
        )).thenReturn(List.of());

        service.recommendPodcasts("user-1", 10, "category-1", true);

        verify(repository).findCandidates(
                "user-1",
                List.of(),
                List.of(),
                "category-1",
                LocalDate.parse("2026-05-27"),
                LocalDate.parse("2026-06-02"),
                Instant.parse("2026-05-03T10:15:30Z"),
                500
        );
        assertThatThrownBy(() -> service.recommendPodcasts("user-1", 101, null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit must be between 1 and 100");
    }

    private PodcastRecommendationCandidate candidate(
            String podcastId,
            String authorId,
            String categoryId,
            String popularity,
            boolean liked,
            boolean disliked,
            long playFinishedCount
    ) {
        return new PodcastRecommendationCandidate(
                podcastId,
                "Podcast " + podcastId,
                authorId,
                categoryId,
                Instant.parse("2026-06-01T00:00:00Z"),
                new BigDecimal(popularity),
                1,
                0,
                0,
                BigDecimal.ZERO,
                liked,
                disliked,
                playFinishedCount,
                playFinishedCount > 0 ? Instant.parse("2026-06-01T01:00:00Z") : null
        );
    }
}
