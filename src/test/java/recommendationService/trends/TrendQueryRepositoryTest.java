package recommendationService.trends;

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
class TrendQueryRepositoryTest {

    private static final String PODCAST_ID = "10000000-0000-0000-0000-000000000001";
    private static final String HIDDEN_PODCAST_ID = "10000000-0000-0000-0000-000000000002";
    private static final String AUTHOR_ID = "20000000-0000-0000-0000-000000000001";
    private static final String CATEGORY_ID = "30000000-0000-0000-0000-000000000001";
    private static final String PLAYLIST_ID = "40000000-0000-0000-0000-000000000001";

    @Autowired
    private TrendQueryRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from playlist_daily_stats");
        jdbcTemplate.update("delete from playlist_catalog_snapshot");
        jdbcTemplate.update("delete from author_daily_stats");
        jdbcTemplate.update("delete from author_catalog_snapshot");
        jdbcTemplate.update("delete from podcast_daily_stats");
        jdbcTemplate.update("delete from podcast_catalog_snapshot");

        jdbcTemplate.update("""
                insert into podcast_catalog_snapshot (podcast_id, author_id, category_id, title, status, published_at, created_at, updated_at)
                values (?, ?, ?, ?, 'PUBLISHED', current_timestamp, current_timestamp, current_timestamp)
                """, PODCAST_ID, AUTHOR_ID, CATEGORY_ID, "Published podcast");
        jdbcTemplate.update("""
                insert into podcast_catalog_snapshot (podcast_id, author_id, category_id, title, status, published_at, created_at, updated_at)
                values (?, ?, ?, ?, 'DELETED', current_timestamp, current_timestamp, current_timestamp)
                """, HIDDEN_PODCAST_ID, AUTHOR_ID, CATEGORY_ID, "Deleted podcast");
        jdbcTemplate.update("""
                insert into podcast_daily_stats (podcast_id, stat_date, play_count, completion_count, like_count, dislike_count, play_finished_count, share_count, rating_count, rating_sum, created_at, updated_at)
                values (?, date '2026-06-02', 5, 1, 2, 0, 3, 0, 0, 0, current_timestamp, current_timestamp)
                """, PODCAST_ID);
        jdbcTemplate.update("""
                insert into podcast_daily_stats (podcast_id, stat_date, play_count, completion_count, like_count, dislike_count, play_finished_count, share_count, rating_count, rating_sum, created_at, updated_at)
                values (?, date '2026-06-02', 100, 0, 50, 0, 0, 0, 0, 0, current_timestamp, current_timestamp)
                """, HIDDEN_PODCAST_ID);

        jdbcTemplate.update("""
                insert into author_catalog_snapshot (author_id, display_name, status, created_at, updated_at)
                values (?, 'Trend Author', 'ACTIVE', current_timestamp, current_timestamp)
                """, AUTHOR_ID);
        jdbcTemplate.update("""
                insert into author_daily_stats (author_id, stat_date, podcast_count, play_count, completion_count, follower_count, followed_count, unfollowed_count, like_count, dislike_count, play_finished_count, rating_count, rating_sum, created_at, updated_at)
                values (?, date '2026-06-02', 0, 10, 0, 2, 2, 0, 3, 0, 1, 0, 0, current_timestamp, current_timestamp)
                """, AUTHOR_ID);

        jdbcTemplate.update("""
                insert into playlist_catalog_snapshot (playlist_id, owner_user_id, title, visibility, status, created_at, updated_at)
                values (?, '50000000-0000-0000-0000-000000000001', 'Trend Playlist', 'PUBLIC', 'ACTIVE', current_timestamp, current_timestamp)
                """, PLAYLIST_ID);
        jdbcTemplate.update("""
                insert into playlist_daily_stats (playlist_id, stat_date, view_count, play_count, follower_count, created_at, updated_at)
                values (?, date '2026-06-02', 4, 5, 2, current_timestamp, current_timestamp)
                """, PLAYLIST_ID);
    }

    @Test
    void podcastTrendsUseDailyStatsSnapshotsCategoryAndPublishedStatus() {
        List<TrendRow> rows = repository.findPodcastTrends(
                LocalDate.parse("2026-06-02"),
                LocalDate.parse("2026-06-02"),
                CATEGORY_ID,
                10
        );

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().itemId()).isEqualTo(PODCAST_ID);
        assertThat(rows.getFirst().metadata()).containsEntry("title", "Published podcast");
    }

    @Test
    void authorTrendsUseDailyStatsAndSnapshotMetadata() {
        List<TrendRow> rows = repository.findAuthorTrends(
                LocalDate.parse("2026-06-02"),
                LocalDate.parse("2026-06-02"),
                10
        );

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().itemId()).isEqualTo(AUTHOR_ID);
        assertThat(rows.getFirst().metadata()).containsEntry("displayName", "Trend Author");
    }

    @Test
    void playlistTrendsUseDailyStatsAndSnapshotMetadata() {
        List<TrendRow> rows = repository.findPlaylistTrends(
                LocalDate.parse("2026-06-02"),
                LocalDate.parse("2026-06-02"),
                10
        );

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().itemId()).isEqualTo(PLAYLIST_ID);
        assertThat(rows.getFirst().metadata()).containsEntry("title", "Trend Playlist");
    }
}
