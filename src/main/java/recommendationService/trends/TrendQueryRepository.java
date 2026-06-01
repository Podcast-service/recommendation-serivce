package recommendationService.trends;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TrendQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public TrendQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TrendRow> findPodcastTrends(
            LocalDate startDate,
            LocalDate endDate,
            String categoryId,
            int limit
    ) {
        String categoryFilter = categoryId == null || categoryId.isBlank()
                ? ""
                : " and p.category_id = ?";
        Object[] args = categoryFilter.isEmpty()
                ? new Object[]{Date.valueOf(startDate), Date.valueOf(endDate), limit}
                : new Object[]{Date.valueOf(startDate), Date.valueOf(endDate), categoryId, limit};

        return jdbcTemplate.query("""
                        select s.podcast_id as item_id,
                               sum(s.play_count + s.play_finished_count * 2 + s.like_count * 3 - s.dislike_count) as score,
                               p.title,
                               p.author_id,
                               p.category_id,
                               p.status,
                               p.published_at
                          from podcast_daily_stats s
                          join podcast_catalog_snapshot p on p.podcast_id = s.podcast_id
                         where s.stat_date between ? and ?
                           and p.status = 'PUBLISHED'
                        """ + categoryFilter + """
                         group by s.podcast_id, p.title, p.author_id, p.category_id, p.status, p.published_at
                        having sum(s.play_count + s.play_finished_count * 2 + s.like_count * 3 - s.dislike_count) > 0
                         order by score desc, s.podcast_id asc
                         limit ?
                        """,
                args,
                (rs, rowNum) -> new TrendRow(
                        rs.getString("item_id"),
                        rs.getBigDecimal("score"),
                        "PODCAST_ACTIVITY",
                        "Подкасты ранжируются по прослушиваниям, завершениям и реакциям за период",
                        Map.of(
                                "title", rs.getString("title"),
                                "authorId", rs.getString("author_id"),
                                "categoryId", rs.getString("category_id"),
                                "status", rs.getString("status"),
                                "publishedAt", nullable(rs.getObject("published_at"))
                        )
                ));
    }

    public List<TrendRow> findAuthorTrends(LocalDate startDate, LocalDate endDate, int limit) {
        return jdbcTemplate.query("""
                        select s.author_id as item_id,
                               sum(s.play_count + s.play_finished_count * 2 + s.like_count * 3 + s.followed_count * 4 - s.unfollowed_count * 2 - s.dislike_count) as score,
                               a.display_name,
                               a.status
                          from author_daily_stats s
                          left join author_catalog_snapshot a on a.author_id = s.author_id
                         where s.stat_date between ? and ?
                         group by s.author_id, a.display_name, a.status
                        having sum(s.play_count + s.play_finished_count * 2 + s.like_count * 3 + s.followed_count * 4 - s.unfollowed_count * 2 - s.dislike_count) > 0
                         order by score desc, s.author_id asc
                         limit ?
                        """,
                (rs, rowNum) -> new TrendRow(
                        rs.getString("item_id"),
                        rs.getBigDecimal("score"),
                        "AUTHOR_ACTIVITY",
                        "Авторы ранжируются по прослушиваниям, реакциям и подпискам за период",
                        Map.of(
                                "displayName", nullable(rs.getString("display_name")),
                                "status", nullable(rs.getString("status"))
                        )
                ),
                Date.valueOf(startDate),
                Date.valueOf(endDate),
                limit
        );
    }

    public List<TrendRow> findPlaylistTrends(LocalDate startDate, LocalDate endDate, int limit) {
        return jdbcTemplate.query("""
                        select s.playlist_id as item_id,
                               sum(s.view_count + s.play_count * 2 + s.follower_count * 3) as score,
                               p.title,
                               p.owner_user_id,
                               p.visibility,
                               p.status
                          from playlist_daily_stats s
                          left join playlist_catalog_snapshot p on p.playlist_id = s.playlist_id
                         where s.stat_date between ? and ?
                           and (p.status is null or p.status <> 'DELETED')
                         group by s.playlist_id, p.title, p.owner_user_id, p.visibility, p.status
                        having sum(s.view_count + s.play_count * 2 + s.follower_count * 3) > 0
                         order by score desc, s.playlist_id asc
                         limit ?
                        """,
                (rs, rowNum) -> new TrendRow(
                        rs.getString("item_id"),
                        rs.getBigDecimal("score"),
                        "PLAYLIST_ACTIVITY",
                        "Плейлисты ранжируются по просмотрам, прослушиваниям и подпискам за период",
                        Map.of(
                                "title", nullable(rs.getString("title")),
                                "ownerUserId", nullable(rs.getString("owner_user_id")),
                                "visibility", nullable(rs.getString("visibility")),
                                "status", nullable(rs.getString("status"))
                        )
                ),
                Date.valueOf(startDate),
                Date.valueOf(endDate),
                limit
        );
    }

    private Object nullable(Object value) {
        return value == null ? "" : value;
    }
}
