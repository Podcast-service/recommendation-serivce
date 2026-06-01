package recommendationService.recommendation;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PodcastRecommendationQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public PodcastRecommendationQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<InterestScore> findTopCategoryInterests(String userId, int limit) {
        return jdbcTemplate.query("""
                        select category_id, interest_score
                          from user_category_interest
                         where user_id = ?
                           and interest_score > 0
                         order by interest_score desc, category_id asc
                         limit ?
                        """,
                (rs, rowNum) -> new InterestScore(rs.getString("category_id"), rs.getBigDecimal("interest_score")),
                userId,
                limit
        );
    }

    public List<InterestScore> findTopAuthorInterests(String userId, int limit) {
        return jdbcTemplate.query("""
                        select author_id, interest_score
                          from user_author_interest
                         where user_id = ?
                           and interest_score > 0
                         order by interest_score desc, author_id asc
                         limit ?
                        """,
                (rs, rowNum) -> new InterestScore(rs.getString("author_id"), rs.getBigDecimal("interest_score")),
                userId,
                limit
        );
    }

    public List<PodcastRecommendationCandidate> findCandidates(
            String userId,
            List<String> categoryIds,
            List<String> authorIds,
            String categoryIdFilter,
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            Instant freshPublishedAfter,
            int candidateLimit
    ) {
        List<Object> args = new ArrayList<>();
        args.add(Date.valueOf(weekStartDate));
        args.add(Date.valueOf(weekEndDate));
        args.add(userId);

        StringBuilder where = new StringBuilder("p.status = 'PUBLISHED'");
        if (categoryIdFilter != null && !categoryIdFilter.isBlank()) {
            where.append(" and p.category_id = ?");
            args.add(categoryIdFilter);
        }

        List<String> candidatePredicates = new ArrayList<>();
        if (!categoryIds.isEmpty()) {
            candidatePredicates.add("p.category_id in (" + placeholders(categoryIds.size()) + ")");
            args.addAll(categoryIds);
        }
        if (!authorIds.isEmpty()) {
            candidatePredicates.add("p.author_id in (" + placeholders(authorIds.size()) + ")");
            args.addAll(authorIds);
        }
        candidatePredicates.add("coalesce(s.popularity_score, 0) > 0");
        if (!categoryIds.isEmpty() || !authorIds.isEmpty()) {
            candidatePredicates.add("p.published_at >= ?");
            args.add(Timestamp.from(freshPublishedAfter));
        }
        where.append(" and (").append(String.join(" or ", candidatePredicates)).append(")");
        args.add(candidateLimit);

        String sql = """
                        select p.podcast_id,
                               p.title,
                               p.author_id,
                               p.category_id,
                               p.published_at,
                               coalesce(s.popularity_score, 0) as popularity_score,
                               coalesce(s.like_count, 0) as like_count,
                               coalesce(s.dislike_count, 0) as dislike_count,
                               coalesce(s.rating_count, 0) as rating_count,
                               coalesce(s.rating_sum, 0) as rating_sum,
                               coalesce(i.liked, false) as liked,
                               coalesce(i.disliked, false) as disliked,
                               coalesce(i.play_finished_count, 0) as play_finished_count,
                               i.last_interaction_at
                          from podcast_catalog_snapshot p
                          left join (
                                select podcast_id,
                                       sum(play_count + play_finished_count * 2 + like_count * 3 - dislike_count) as popularity_score,
                                       sum(like_count) as like_count,
                                       sum(dislike_count) as dislike_count,
                                       sum(rating_count) as rating_count,
                                       sum(rating_sum) as rating_sum
                                  from podcast_daily_stats
                                 where stat_date between ? and ?
                                 group by podcast_id
                          ) s on s.podcast_id = p.podcast_id
                          left join user_podcast_interaction i
                            on i.user_id = ? and i.podcast_id = p.podcast_id
                        """ + " where " + where + """
                         order by coalesce(s.popularity_score, 0) desc,
                                  p.published_at desc nulls last,
                                  p.podcast_id asc
                         limit ?
                        """;
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> new PodcastRecommendationCandidate(
                        rs.getString("podcast_id"),
                        rs.getString("title"),
                        rs.getString("author_id"),
                        rs.getString("category_id"),
                        rs.getTimestamp("published_at") == null ? null : rs.getTimestamp("published_at").toInstant(),
                        rs.getBigDecimal("popularity_score"),
                        rs.getLong("like_count"),
                        rs.getLong("dislike_count"),
                        rs.getLong("rating_count"),
                        rs.getBigDecimal("rating_sum"),
                        rs.getBoolean("liked"),
                        rs.getBoolean("disliked"),
                        rs.getLong("play_finished_count"),
                        rs.getTimestamp("last_interaction_at") == null ? null : rs.getTimestamp("last_interaction_at").toInstant()
                ),
                args.toArray()
        );
    }

    private String placeholders(int size) {
        return String.join(", ", java.util.Collections.nCopies(size, "?"));
    }
}
