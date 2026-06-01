package recommendationService.recommendation;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RecommendationBlocksQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public RecommendationBlocksQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PlaylistRecommendationCandidate> findPlaylistCandidates(
            String userId,
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            int candidateLimit
    ) {
        return jdbcTemplate.query("""
                        select playlist.playlist_id,
                               playlist.title,
                               playlist.owner_user_id,
                               playlist.updated_at,
                               coalesce(sum(category_interest.interest_score), 0) as category_interest_score,
                               coalesce(sum(author_interest.interest_score), 0) as author_interest_score,
                               coalesce(max(playlist_stats.popularity_score), 0) as popularity_score,
                               coalesce(avg(coalesce(podcast_quality.quality_score, 0.5)), 0.5) as quality_score
                          from playlist_catalog_snapshot playlist
                          join playlist_item_snapshot playlist_item
                            on playlist_item.playlist_id = playlist.playlist_id
                          join podcast_catalog_snapshot podcast
                            on podcast.podcast_id = playlist_item.podcast_id
                           and podcast.status = 'PUBLISHED'
                          left join user_category_interest category_interest
                            on category_interest.user_id = ?
                           and category_interest.category_id = podcast.category_id
                           and category_interest.interest_score > 0
                          left join user_author_interest author_interest
                            on author_interest.user_id = ?
                           and author_interest.author_id = podcast.author_id
                           and author_interest.interest_score > 0
                          left join (
                                select playlist_id,
                                       sum(view_count + play_count * 2 + follower_count * 3) as popularity_score
                                  from playlist_daily_stats
                                 where stat_date between ? and ?
                                 group by playlist_id
                          ) playlist_stats on playlist_stats.playlist_id = playlist.playlist_id
                          left join (
                                select podcast_id,
                                       case
                                           when sum(like_count + dislike_count) > 0
                                           then sum(like_count) * 1.0 / sum(like_count + dislike_count)
                                           else 0.5
                                       end as quality_score
                                  from podcast_daily_stats
                                 where stat_date between ? and ?
                                 group by podcast_id
                          ) podcast_quality on podcast_quality.podcast_id = podcast.podcast_id
                         where playlist.status <> 'DELETED'
                         group by playlist.playlist_id, playlist.title, playlist.owner_user_id, playlist.updated_at
                        having count(podcast.podcast_id) > 0
                         order by popularity_score desc, playlist.updated_at desc, playlist.playlist_id asc
                         limit ?
                        """,
                (rs, rowNum) -> new PlaylistRecommendationCandidate(
                        rs.getString("playlist_id"),
                        rs.getString("title"),
                        rs.getString("owner_user_id"),
                        rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toInstant(),
                        rs.getBigDecimal("category_interest_score"),
                        rs.getBigDecimal("author_interest_score"),
                        rs.getBigDecimal("popularity_score"),
                        rs.getBigDecimal("quality_score")
                ),
                userId,
                userId,
                Date.valueOf(weekStartDate),
                Date.valueOf(weekEndDate),
                Date.valueOf(weekStartDate),
                Date.valueOf(weekEndDate),
                candidateLimit
        );
    }

    public Optional<SimilarPodcastSource> findSimilarPodcastSource(String podcastId) {
        return jdbcTemplate.query("""
                        select podcast_id, author_id, category_id, tags, duration_seconds
                          from podcast_catalog_snapshot
                         where podcast_id = ?
                           and status = 'PUBLISHED'
                        """,
                (rs, rowNum) -> new SimilarPodcastSource(
                        rs.getString("podcast_id"),
                        rs.getString("author_id"),
                        rs.getString("category_id"),
                        rs.getString("tags"),
                        (Integer) rs.getObject("duration_seconds")
                ),
                podcastId
        ).stream().findFirst();
    }

    public List<SimilarPodcastCandidate> findSimilarPodcastCandidates(
            String podcastId,
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            int candidateLimit
    ) {
        return jdbcTemplate.query("""
                        select podcast.podcast_id,
                               podcast.title,
                               podcast.author_id,
                               podcast.category_id,
                               podcast.tags,
                               podcast.duration_seconds,
                               podcast.published_at,
                               coalesce(stats.popularity_score, 0) as popularity_score
                          from podcast_catalog_snapshot podcast
                          left join (
                                select podcast_id,
                                       sum(play_count + play_finished_count * 2 + like_count * 3 - dislike_count) as popularity_score
                                  from podcast_daily_stats
                                 where stat_date between ? and ?
                                 group by podcast_id
                          ) stats on stats.podcast_id = podcast.podcast_id
                         where podcast.status = 'PUBLISHED'
                           and podcast.podcast_id <> ?
                         order by popularity_score desc, podcast.published_at desc, podcast.podcast_id asc
                         limit ?
                        """,
                (rs, rowNum) -> new SimilarPodcastCandidate(
                        rs.getString("podcast_id"),
                        rs.getString("title"),
                        rs.getString("author_id"),
                        rs.getString("category_id"),
                        rs.getString("tags"),
                        (Integer) rs.getObject("duration_seconds"),
                        rs.getTimestamp("published_at") == null ? null : rs.getTimestamp("published_at").toInstant(),
                        rs.getBigDecimal("popularity_score")
                ),
                Date.valueOf(weekStartDate),
                Date.valueOf(weekEndDate),
                podcastId,
                candidateLimit
        );
    }

    public List<SimilarAuthorCandidate> findSimilarAuthorCandidates(
            String authorId,
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            int candidateLimit
    ) {
        return jdbcTemplate.query("""
                        select author.author_id,
                               author.display_name,
                               count(distinct podcast.category_id) as category_score,
                               coalesce(max(audience.shared_user_count), 0) as audience_score,
                               coalesce(max(trend.trend_score), 0) as trend_score
                          from author_catalog_snapshot author
                          join podcast_catalog_snapshot podcast
                            on podcast.author_id = author.author_id
                           and podcast.status = 'PUBLISHED'
                           and podcast.category_id in (
                                select distinct source_podcast.category_id
                                  from podcast_catalog_snapshot source_podcast
                                 where source_podcast.author_id = ?
                                   and source_podcast.status = 'PUBLISHED'
                                   and source_podcast.category_id is not null
                           )
                          left join (
                                select candidate.author_id,
                                       count(distinct candidate.user_id) as shared_user_count
                                  from user_author_interest source
                                  join user_author_interest candidate
                                    on candidate.user_id = source.user_id
                                   and candidate.author_id <> source.author_id
                                   and candidate.interest_score > 0
                                 where source.author_id = ?
                                   and source.interest_score > 0
                                 group by candidate.author_id
                          ) audience on audience.author_id = author.author_id
                          left join (
                                select author_id,
                                       sum(play_count + play_finished_count * 2 + like_count * 3 + followed_count * 4 - unfollowed_count * 2 - dislike_count) as trend_score
                                  from author_daily_stats
                                 where stat_date between ? and ?
                                 group by author_id
                          ) trend on trend.author_id = author.author_id
                         where author.author_id <> ?
                           and author.status <> 'DELETED'
                         group by author.author_id, author.display_name
                         order by category_score desc, audience_score desc, trend_score desc, author.author_id asc
                         limit ?
                        """,
                (rs, rowNum) -> new SimilarAuthorCandidate(
                        rs.getString("author_id"),
                        rs.getString("display_name"),
                        BigDecimal.valueOf(rs.getLong("category_score")),
                        BigDecimal.valueOf(rs.getLong("audience_score")),
                        rs.getBigDecimal("trend_score")
                ),
                authorId,
                authorId,
                Date.valueOf(weekStartDate),
                Date.valueOf(weekEndDate),
                authorId,
                candidateLimit
        );
    }
}
