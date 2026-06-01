package recommendationService.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import recommendationService.recommendation.scoring.PlaylistRecommendationScorer;
import recommendationService.recommendation.scoring.SimilarityScorer;

class RecommendationBlocksServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-02T10:15:30Z"), ZoneOffset.UTC);

    private final RecommendationBlocksQueryRepository repository = Mockito.mock(RecommendationBlocksQueryRepository.class);
    private final PodcastRecommendationService podcastRecommendationService = Mockito.mock(PodcastRecommendationService.class);
    private final RecommendationBlocksService service = new RecommendationBlocksService(
            repository,
            podcastRecommendationService,
            CLOCK,
            new PlaylistRecommendationScorer(),
            new SimilarityScorer()
    );

    @Test
    void feedCombinesPodcastAndPlaylistItems() {
        when(podcastRecommendationService.recommendPodcasts("user-1", 20, null, true))
                .thenReturn(List.of(new PodcastRecommendationResponse(
                        "podcast-1",
                        1,
                        new BigDecimal("70.0000"),
                        "TOP_CATEGORY",
                        "reason",
                        Map.of("title", "Podcast")
                )));
        when(repository.findPlaylistCandidates(
                "user-1",
                LocalDate.parse("2026-05-27"),
                LocalDate.parse("2026-06-02"),
                200
        )).thenReturn(List.of(new PlaylistRecommendationCandidate(
                "playlist-1",
                "Playlist",
                "owner-1",
                Instant.parse("2026-06-01T00:00:00Z"),
                new BigDecimal("10"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("0.5")
        )));

        List<RecommendationItemResponse> feed = service.feed("user-1", null);

        assertThat(feed).extracting(RecommendationItemResponse::itemType)
                .contains("PODCAST", "PLAYLIST");
        assertThat(feed).extracting(RecommendationItemResponse::rank)
                .containsExactly(1, 2);
    }

    @Test
    void playlistRecommendationsRespectUserInterests() {
        when(repository.findPlaylistCandidates(
                "user-1",
                LocalDate.parse("2026-05-27"),
                LocalDate.parse("2026-06-02"),
                200
        )).thenReturn(List.of(
                new PlaylistRecommendationCandidate(
                        "playlist-category",
                        "Category playlist",
                        "owner-1",
                        Instant.parse("2026-06-01T00:00:00Z"),
                        new BigDecimal("10"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        new BigDecimal("0.5")
                ),
                new PlaylistRecommendationCandidate(
                        "playlist-popular",
                        "Popular playlist",
                        "owner-2",
                        Instant.parse("2026-06-01T00:00:00Z"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        new BigDecimal("100"),
                        new BigDecimal("0.5")
                )
        ));

        List<RecommendationItemResponse> playlists = service.playlists("user-1", null);

        assertThat(playlists.getFirst().itemId()).isEqualTo("playlist-category");
        assertThat(playlists.getFirst().reasonCode()).isEqualTo("PLAYLIST_TOP_CATEGORY");
    }

    @Test
    void similarPodcastsRespectCategoryAuthorTagsDuration() {
        SimilarPodcastSource source = new SimilarPodcastSource(
                "podcast-source",
                "author-1",
                "category-1",
                "java,backend,architecture",
                1800
        );
        when(repository.findSimilarPodcastSource("podcast-source"))
                .thenReturn(Optional.of(source));
        when(repository.findSimilarPodcastCandidates(
                source,
                LocalDate.parse("2026-05-27"),
                LocalDate.parse("2026-06-02"),
                200
        )).thenReturn(List.of(
                new SimilarPodcastCandidate(
                        "podcast-strong",
                        "Strong",
                        "author-1",
                        "category-1",
                        "java,backend",
                        1780,
                        Instant.parse("2026-06-01T00:00:00Z"),
                        new BigDecimal("5")
                ),
                new SimilarPodcastCandidate(
                        "podcast-weak",
                        "Weak",
                        "author-2",
                        "category-2",
                        "design",
                        600,
                        Instant.parse("2026-06-01T00:00:00Z"),
                        new BigDecimal("100")
                )
        ));

        List<RecommendationItemResponse> similar = service.similarPodcasts("podcast-source", null);

        assertThat(similar.getFirst().itemId()).isEqualTo("podcast-strong");
        assertThat(similar.getFirst().reasonCode()).isEqualTo("SAME_CATEGORY");
    }

    @Test
    void similarAuthorsWorksWithFallback() {
        when(repository.findSimilarAuthorCandidates(
                "author-source",
                LocalDate.parse("2026-05-27"),
                LocalDate.parse("2026-06-02"),
                200
        )).thenReturn(List.of(new SimilarAuthorCandidate(
                "author-fallback",
                "Fallback Author",
                BigDecimal.ONE,
                BigDecimal.ZERO,
                new BigDecimal("20")
        )));

        List<RecommendationItemResponse> similar = service.similarAuthors("author-source", null);

        assertThat(similar).hasSize(1);
        assertThat(similar.getFirst().itemId()).isEqualTo("author-fallback");
        assertThat(similar.getFirst().reasonCode()).isEqualTo("FALLBACK_CATEGORY_TREND");
    }

    @Test
    void rejectsLimitAboveMax() {
        assertThatThrownBy(() -> service.feed("user-1", 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit must be between 1 and 100");
    }
}
