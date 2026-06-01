package recommendationService.recommendation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PodcastRecommendationCacheRepository {

    static final String PERSONAL_PODCASTS = "PERSONAL_PODCASTS";
    static final String GLOBAL_PODCASTS = "GLOBAL_PODCASTS";

    private static final TypeReference<Map<String, Object>> METADATA_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public PodcastRecommendationCacheRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<PodcastRecommendationResponse> findPersonalPodcasts(
            String userId,
            String requestKey,
            Instant now,
            int limit,
            boolean excludeSeen
    ) {
        return jdbcTemplate.query("""
                        select item_id, item_rank, score, reason_code, reason_text, payload
                          from recommendation_cache cache
                          join podcast_catalog_snapshot podcast on podcast.podcast_id = cache.item_id
                          left join user_podcast_interaction interaction
                            on interaction.user_id = ? and interaction.podcast_id = cache.item_id
                         where cache.user_id = ?
                           and recommendation_type = ?
                           and cache_key like ?
                           and expires_at > ?
                           and podcast.status = 'PUBLISHED'
                           and coalesce(interaction.disliked, false) = false
                           and (? = false or interaction.interaction_id is null)
                         order by item_rank asc
                         limit ?
                        """,
                (rs, rowNum) -> new PodcastRecommendationResponse(
                        rs.getString("item_id"),
                        rs.getInt("item_rank"),
                        rs.getBigDecimal("score"),
                        rs.getString("reason_code"),
                        rs.getString("reason_text"),
                        readMetadata(rs.getString("payload"))
                ),
                userId,
                userId,
                PERSONAL_PODCASTS,
                requestKey + ":%",
                Timestamp.from(now),
                excludeSeen,
                limit
        );
    }

    public List<PodcastRecommendationResponse> findGlobalPodcasts(
            String userId,
            String requestKey,
            Instant now,
            int limit,
            boolean excludeSeen
    ) {
        return jdbcTemplate.query("""
                        select item_id, item_rank, score, reason_code, reason_text, payload
                          from global_recommendation_cache cache
                          join podcast_catalog_snapshot podcast on podcast.podcast_id = cache.item_id
                          left join user_podcast_interaction interaction
                            on interaction.user_id = ? and interaction.podcast_id = cache.item_id
                         where recommendation_type = ?
                           and cache_key like ?
                           and expires_at > ?
                           and podcast.status = 'PUBLISHED'
                           and coalesce(interaction.disliked, false) = false
                           and (? = false or interaction.interaction_id is null)
                         order by item_rank asc
                         limit ?
                        """,
                (rs, rowNum) -> new PodcastRecommendationResponse(
                        rs.getString("item_id"),
                        rs.getInt("item_rank"),
                        rs.getBigDecimal("score"),
                        rs.getString("reason_code"),
                        rs.getString("reason_text"),
                        readMetadata(rs.getString("payload"))
                ),
                userId,
                GLOBAL_PODCASTS,
                requestKey + ":%",
                Timestamp.from(now),
                excludeSeen,
                limit
        );
    }

    @Transactional
    public void replacePersonalPodcasts(
            String userId,
            String requestKey,
            List<PodcastRecommendationResponse> recommendations,
            Instant generatedAt,
            Instant expiresAt
    ) {
        jdbcTemplate.update("""
                        delete from recommendation_cache
                         where user_id = ?
                           and recommendation_type = ?
                           and cache_key like ?
                        """,
                userId,
                PERSONAL_PODCASTS,
                requestKey + ":%"
        );
        recommendations.forEach(recommendation -> jdbcTemplate.update("""
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
                PERSONAL_PODCASTS,
                cacheEntryKey(requestKey, recommendation.rank()),
                writeMetadata(recommendation.metadata()),
                Timestamp.from(generatedAt),
                Timestamp.from(expiresAt),
                Timestamp.from(generatedAt),
                Timestamp.from(generatedAt),
                recommendation.itemId(),
                recommendation.rank(),
                recommendation.score(),
                recommendation.reasonCode(),
                recommendation.reasonText()
        ));
    }

    @Transactional
    public void replaceGlobalPodcasts(
            String requestKey,
            List<PodcastRecommendationResponse> recommendations,
            Instant generatedAt,
            Instant expiresAt
    ) {
        jdbcTemplate.update("""
                        delete from global_recommendation_cache
                         where recommendation_type = ?
                           and cache_key like ?
                        """,
                GLOBAL_PODCASTS,
                requestKey + ":%"
        );
        recommendations.forEach(recommendation -> jdbcTemplate.update("""
                        insert into global_recommendation_cache (
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
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                GLOBAL_PODCASTS,
                cacheEntryKey(requestKey, recommendation.rank()),
                writeMetadata(recommendation.metadata()),
                Timestamp.from(generatedAt),
                Timestamp.from(expiresAt),
                Timestamp.from(generatedAt),
                Timestamp.from(generatedAt),
                recommendation.itemId(),
                recommendation.rank(),
                recommendation.score(),
                recommendation.reasonCode(),
                recommendation.reasonText()
        ));
    }

    @Transactional
    public int cleanupExpired(Instant now) {
        int personalDeleted = jdbcTemplate.update("delete from recommendation_cache where expires_at <= ?", Timestamp.from(now));
        int globalDeleted = jdbcTemplate.update("delete from global_recommendation_cache where expires_at <= ?", Timestamp.from(now));
        return personalDeleted + globalDeleted;
    }

    public List<String> findUserIdsForRefresh(int limit) {
        return jdbcTemplate.queryForList("""
                        select user_id
                          from (
                                select user_id from user_category_interest
                                union
                                select user_id from user_author_interest
                                union
                                select user_id from user_podcast_interaction
                          ) users
                         order by user_id asc
                         limit ?
                        """,
                String.class,
                limit
        );
    }

    private String cacheEntryKey(String requestKey, int rank) {
        return requestKey + ":rank=" + String.format("%03d", rank);
    }

    private Map<String, Object> readMetadata(String payload) {
        try {
            return objectMapper.readValue(payload, METADATA_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to read recommendation cache payload", exception);
        }
    }

    private String writeMetadata(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to write recommendation cache payload", exception);
        }
    }
}
