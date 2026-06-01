package recommendationService.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PodcastRecommendationQueryRepositoryTest {

    @Autowired
    private PodcastRecommendationQueryRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from user_podcast_interaction");
        jdbcTemplate.update("delete from user_author_interest");
        jdbcTemplate.update("delete from user_category_interest");
        jdbcTemplate.update("delete from podcast_daily_stats");
        jdbcTemplate.update("delete from podcast_catalog_snapshot");

        jdbcTemplate.update("""
                insert into user_category_interest (user_id, category_id, interest_score, signal_count, created_at, updated_at)
                values ('user-1', 'category-1', 10, 1, current_timestamp, current_timestamp)
                """);
        jdbcTemplate.update("""
                insert into user_author_interest (user_id, author_id, interest_score, signal_count, created_at, updated_at)
                values ('user-1', 'author-1', 8, 1, current_timestamp, current_timestamp)
                """);
        jdbcTemplate.update("""
                insert into podcast_catalog_snapshot (podcast_id, author_id, category_id, title, status, published_at, created_at, updated_at)
                values ('podcast-category', 'author-2', 'category-1', 'Category podcast', 'PUBLISHED', current_timestamp, current_timestamp, current_timestamp)
                """);
        jdbcTemplate.update("""
                insert into podcast_catalog_snapshot (podcast_id, author_id, category_id, title, status, published_at, created_at, updated_at)
                values ('podcast-author', 'author-1', 'category-2', 'Author podcast', 'PUBLISHED', current_timestamp, current_timestamp, current_timestamp)
                """);
        jdbcTemplate.update("""
                insert into podcast_catalog_snapshot (podcast_id, author_id, category_id, title, status, published_at, created_at, updated_at)
                values ('podcast-popular', 'author-3', 'category-3', 'Popular podcast', 'PUBLISHED', current_timestamp, current_timestamp, current_timestamp)
                """);
        jdbcTemplate.update("""
                insert into podcast_catalog_snapshot (podcast_id, author_id, category_id, title, status, published_at, created_at, updated_at)
                values ('podcast-deleted', 'author-3', 'category-1', 'Deleted podcast', 'DELETED', current_timestamp, current_timestamp, current_timestamp)
                """);
        jdbcTemplate.update("""
                insert into podcast_daily_stats (podcast_id, stat_date, play_count, completion_count, like_count, dislike_count, play_finished_count, share_count, rating_count, rating_sum, created_at, updated_at)
                values ('podcast-popular', date '2026-06-02', 10, 0, 5, 0, 1, 0, 0, 0, current_timestamp, current_timestamp)
                """);
        jdbcTemplate.update("""
                insert into user_podcast_interaction (interaction_id, user_id, podcast_id, interaction_type, occurred_at, created_at, liked, disliked, play_finished_count, last_interaction_at)
                values ('user-1:podcast-category', 'user-1', 'podcast-category', 'ACTIVITY', current_timestamp, current_timestamp, false, false, 1, current_timestamp)
                """);
    }

    @Test
    void findsProfilePopularAndFreshCandidatesWithoutDeletedSnapshots() {
        List<PodcastRecommendationCandidate> candidates = repository.findCandidates(
                "user-1",
                List.of("category-1"),
                List.of("author-1"),
                null,
                LocalDate.parse("2026-05-27"),
                LocalDate.parse("2026-06-02"),
                Instant.parse("2026-05-03T00:00:00Z"),
                10
        );

        assertThat(candidates).extracting(PodcastRecommendationCandidate::podcastId)
                .contains("podcast-category", "podcast-author", "podcast-popular")
                .doesNotContain("podcast-deleted");
        assertThat(candidates.stream()
                .filter(candidate -> candidate.podcastId().equals("podcast-category"))
                .findFirst()
                .orElseThrow()
                .playFinishedCount()).isEqualTo(1);
    }

    @Test
    void categoryFilterRestrictsCandidates() {
        List<PodcastRecommendationCandidate> candidates = repository.findCandidates(
                "user-1",
                List.of("category-1"),
                List.of("author-1"),
                "category-1",
                LocalDate.parse("2026-05-27"),
                LocalDate.parse("2026-06-02"),
                Instant.parse("2026-05-03T00:00:00Z"),
                10
        );

        assertThat(candidates).extracting(PodcastRecommendationCandidate::podcastId)
                .containsExactly("podcast-category");
    }

    @Test
    void emptyProfileCandidatesUseOnlyGlobalPopular() {
        List<PodcastRecommendationCandidate> candidates = repository.findCandidates(
                "user-2",
                List.of(),
                List.of(),
                null,
                LocalDate.parse("2026-05-27"),
                LocalDate.parse("2026-06-02"),
                Instant.parse("2026-05-03T00:00:00Z"),
                10
        );

        assertThat(candidates).extracting(PodcastRecommendationCandidate::podcastId)
                .containsExactly("podcast-popular");
    }
}
