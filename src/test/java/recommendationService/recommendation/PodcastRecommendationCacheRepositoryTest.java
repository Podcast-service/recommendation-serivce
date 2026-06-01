package recommendationService.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PodcastRecommendationCacheRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-06-02T10:15:30Z");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private PodcastRecommendationCacheRepository repository;

    @BeforeEach
    void setUp() {
        repository = new PodcastRecommendationCacheRepository(jdbcTemplate, objectMapper);
        jdbcTemplate.update("delete from recommendation_cache");
        jdbcTemplate.update("delete from global_recommendation_cache");
        jdbcTemplate.update("delete from user_podcast_interaction");
        jdbcTemplate.update("delete from podcast_catalog_snapshot");
        insertPublishedPodcast("podcast-1");
        insertPublishedPodcast("podcast-2");
    }

    @Test
    void replacePersonalPodcastsWritesAndReadsRankedEntries() {
        List<PodcastRecommendationResponse> recommendations = List.of(
                response("podcast-1", 1),
                response("podcast-2", 2)
        );

        repository.replacePersonalPodcasts(
                "user-1",
                "podcasts:v1:test",
                recommendations,
                NOW,
                NOW.plusSeconds(600)
        );

        List<PodcastRecommendationResponse> cached = repository.findPersonalPodcasts(
                "user-1",
                "podcasts:v1:test",
                NOW,
                20,
                true
        );

        assertThat(cached).extracting(PodcastRecommendationResponse::itemId)
                .containsExactly("podcast-1", "podcast-2");
        assertThat(cached.getFirst().rank()).isEqualTo(1);
        assertThat(cached.getFirst().score()).isEqualByComparingTo("80.0000");
        assertThat(cached.getFirst().reasonCode()).isEqualTo("TOP_CATEGORY");
        assertThat(cached.getFirst().metadata()).containsEntry("title", "Podcast podcast-1");
    }

    @Test
    void expiredCacheIsIgnored() {
        insertExpiredPersonalCache("user-1", "podcasts:v1:expired:rank=001");

        List<PodcastRecommendationResponse> cached = repository.findPersonalPodcasts(
                "user-1",
                "podcasts:v1:expired",
                NOW,
                20,
                true
        );

        assertThat(cached).isEmpty();
    }

    @Test
    void cleanupRemovesExpiredEntries() {
        insertExpiredPersonalCache("user-1", "podcasts:v1:expired:rank=001");
        repository.replaceGlobalPodcasts(
                "podcasts:v1:global",
                List.of(response("podcast-global", 1)),
                NOW,
                NOW.plusSeconds(600)
        );

        int deleted = repository.cleanupExpired(NOW);

        assertThat(deleted).isEqualTo(1);
        Integer personalCount = jdbcTemplate.queryForObject("select count(*) from recommendation_cache", Integer.class);
        Integer globalCount = jdbcTemplate.queryForObject("select count(*) from global_recommendation_cache", Integer.class);
        assertThat(personalCount).isZero();
        assertThat(globalCount).isEqualTo(1);
    }

    @Test
    void cachedDislikedPodcastIsIgnored() {
        repository.replacePersonalPodcasts(
                "user-1",
                "podcasts:v1:test",
                List.of(response("podcast-1", 1)),
                NOW,
                NOW.plusSeconds(600)
        );
        jdbcTemplate.update("""
                insert into user_podcast_interaction (
                    interaction_id, user_id, podcast_id, interaction_type, occurred_at, created_at, disliked
                ) values ('user-1:podcast-1', 'user-1', 'podcast-1', 'DISLIKED', current_timestamp, current_timestamp, true)
                """);

        assertThat(repository.findPersonalPodcasts("user-1", "podcasts:v1:test", NOW, 20, true)).isEmpty();
    }

    @Test
    void cachedDeletedPodcastIsIgnored() {
        repository.replacePersonalPodcasts(
                "user-1",
                "podcasts:v1:test",
                List.of(response("podcast-1", 1)),
                NOW,
                NOW.plusSeconds(600)
        );
        jdbcTemplate.update("update podcast_catalog_snapshot set status = 'DELETED' where podcast_id = 'podcast-1'");

        assertThat(repository.findPersonalPodcasts("user-1", "podcasts:v1:test", NOW, 20, true)).isEmpty();
    }

    private void insertExpiredPersonalCache(String userId, String cacheKey) {
        jdbcTemplate.update("""
                        insert into recommendation_cache (
                            user_id,
                            recommendation_type,
                            cache_key,
                            payload,
                            generated_at,
                            expires_at,
                            created_at,
                            updated_at,
                            item_id,
                            item_rank,
                            score,
                            reason_code,
                            reason_text
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                userId,
                PodcastRecommendationCacheRepository.PERSONAL_PODCASTS,
                cacheKey,
                "{\"title\":\"Expired\"}",
                Timestamp.from(NOW.minusSeconds(1200)),
                Timestamp.from(NOW.minusSeconds(600)),
                Timestamp.from(NOW.minusSeconds(1200)),
                Timestamp.from(NOW.minusSeconds(1200)),
                "podcast-expired",
                1,
                new BigDecimal("10.0000"),
                "POPULAR_NOW",
                "expired"
        );
    }

    private void insertPublishedPodcast(String podcastId) {
        jdbcTemplate.update("""
                insert into podcast_catalog_snapshot (podcast_id, title, status, created_at, updated_at)
                values (?, ?, 'PUBLISHED', current_timestamp, current_timestamp)
                """, podcastId, "Podcast " + podcastId);
    }

    private PodcastRecommendationResponse response(String itemId, int rank) {
        return new PodcastRecommendationResponse(
                itemId,
                rank,
                new BigDecimal("80.0000"),
                "TOP_CATEGORY",
                "reason",
                Map.of("title", "Podcast " + itemId)
        );
    }
}
