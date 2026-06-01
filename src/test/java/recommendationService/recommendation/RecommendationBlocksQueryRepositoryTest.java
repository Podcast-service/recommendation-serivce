package recommendationService.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class RecommendationBlocksQueryRepositoryTest {

    @Autowired
    private RecommendationBlocksQueryRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from user_author_interest");
        jdbcTemplate.update("delete from user_category_interest");
        jdbcTemplate.update("delete from playlist_daily_stats");
        jdbcTemplate.update("delete from playlist_item_snapshot");
        jdbcTemplate.update("delete from playlist_catalog_snapshot");
        jdbcTemplate.update("delete from author_daily_stats");
        jdbcTemplate.update("delete from author_catalog_snapshot");
        jdbcTemplate.update("delete from podcast_daily_stats");
        jdbcTemplate.update("delete from podcast_catalog_snapshot");

        insertPodcast("podcast-source", "author-source", "category-a", "PUBLISHED", "java,backend", 1800);
        insertPodcast("podcast-same", "author-source", "category-a", "PUBLISHED", "java,cloud", 1790);
        insertPodcast("podcast-deleted", "author-source", "category-a", "DELETED", "java", 1800);
        insertPodcast("podcast-draft", "author-other", "category-a", "ACTIVE", "java", 1800);
        insertPodcast("podcast-playlist", "author-playlist", "category-b", "PUBLISHED", "music", 1200);

        jdbcTemplate.update("""
                insert into podcast_daily_stats (podcast_id, stat_date, play_count, completion_count, like_count, dislike_count, play_finished_count, share_count, rating_count, rating_sum, created_at, updated_at)
                values ('podcast-same', date '2026-06-02', 10, 0, 5, 0, 2, 0, 0, 0, current_timestamp, current_timestamp)
                """);

        jdbcTemplate.update("""
                insert into playlist_catalog_snapshot (playlist_id, owner_user_id, title, visibility, status, created_at, updated_at)
                values ('playlist-good', 'owner-1', 'Good Playlist', 'PUBLIC', 'ACTIVE', current_timestamp, current_timestamp)
                """);
        jdbcTemplate.update("""
                insert into playlist_catalog_snapshot (playlist_id, owner_user_id, title, visibility, status, created_at, updated_at)
                values ('playlist-deleted', 'owner-1', 'Deleted Playlist', 'PUBLIC', 'DELETED', current_timestamp, current_timestamp)
                """);
        jdbcTemplate.update("""
                insert into playlist_item_snapshot (playlist_id, podcast_id, item_position, created_at, updated_at)
                values ('playlist-good', 'podcast-playlist', 1, current_timestamp, current_timestamp)
                """);
        jdbcTemplate.update("""
                insert into playlist_item_snapshot (playlist_id, podcast_id, item_position, created_at, updated_at)
                values ('playlist-deleted', 'podcast-playlist', 1, current_timestamp, current_timestamp)
                """);
        jdbcTemplate.update("""
                insert into user_category_interest (user_id, category_id, interest_score, signal_count, created_at, updated_at)
                values ('user-1', 'category-b', 10, 1, current_timestamp, current_timestamp)
                """);

        jdbcTemplate.update("""
                insert into author_catalog_snapshot (author_id, display_name, status, created_at, updated_at)
                values ('author-source', 'Source Author', 'ACTIVE', current_timestamp, current_timestamp)
                """);
        jdbcTemplate.update("""
                insert into author_catalog_snapshot (author_id, display_name, status, created_at, updated_at)
                values ('author-similar', 'Similar Author', 'ACTIVE', current_timestamp, current_timestamp)
                """);
        jdbcTemplate.update("""
                insert into author_catalog_snapshot (author_id, display_name, status, created_at, updated_at)
                values ('author-deleted', 'Deleted Author', 'DELETED', current_timestamp, current_timestamp)
                """);
        insertPodcast("podcast-author-similar", "author-similar", "category-a", "PUBLISHED", "java", 1600);
        insertPodcast("podcast-author-deleted", "author-deleted", "category-a", "PUBLISHED", "java", 1600);
        jdbcTemplate.update("""
                insert into author_daily_stats (author_id, stat_date, podcast_count, play_count, completion_count, follower_count, followed_count, unfollowed_count, like_count, dislike_count, play_finished_count, rating_count, rating_sum, created_at, updated_at)
                values ('author-similar', date '2026-06-02', 1, 10, 0, 0, 1, 0, 2, 0, 1, 0, 0, current_timestamp, current_timestamp)
                """);
    }

    @Test
    void similarPodcastsExcludeSourceAndUnpublishedItems() {
        List<SimilarPodcastCandidate> candidates = repository.findSimilarPodcastCandidates(
                "podcast-source",
                LocalDate.parse("2026-05-27"),
                LocalDate.parse("2026-06-02"),
                10
        );

        assertThat(candidates).extracting(SimilarPodcastCandidate::podcastId)
                .contains("podcast-same")
                .doesNotContain("podcast-source", "podcast-deleted", "podcast-draft");
    }

    @Test
    void playlistCandidatesExcludeDeletedPlaylistsAndUnpublishedPodcasts() {
        List<PlaylistRecommendationCandidate> candidates = repository.findPlaylistCandidates(
                "user-1",
                LocalDate.parse("2026-05-27"),
                LocalDate.parse("2026-06-02"),
                10
        );

        assertThat(candidates).extracting(PlaylistRecommendationCandidate::playlistId)
                .containsExactly("playlist-good");
        assertThat(candidates.getFirst().categoryInterestScore()).isPositive();
    }

    @Test
    void similarAuthorsExcludeSourceAndDeletedAuthors() {
        List<SimilarAuthorCandidate> candidates = repository.findSimilarAuthorCandidates(
                "author-source",
                LocalDate.parse("2026-05-27"),
                LocalDate.parse("2026-06-02"),
                10
        );

        assertThat(candidates).extracting(SimilarAuthorCandidate::authorId)
                .contains("author-similar")
                .doesNotContain("author-source", "author-deleted");
    }

    private void insertPodcast(String podcastId, String authorId, String categoryId, String status, String tags, int durationSeconds) {
        jdbcTemplate.update("""
                insert into podcast_catalog_snapshot (
                    podcast_id,
                    author_id,
                    category_id,
                    title,
                    status,
                    published_at,
                    created_at,
                    updated_at,
                    tags,
                    duration_seconds
                ) values (?, ?, ?, ?, ?, current_timestamp, current_timestamp, current_timestamp, ?, ?)
                """,
                podcastId,
                authorId,
                categoryId,
                "Podcast " + podcastId,
                status,
                tags,
                durationSeconds
        );
    }
}
