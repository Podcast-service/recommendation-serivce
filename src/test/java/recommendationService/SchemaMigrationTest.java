package recommendationService;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SchemaMigrationTest {

    private static final Set<String> EXPECTED_TABLES = Set.of(
            "processed_events",
            "podcast_catalog_snapshot",
            "author_catalog_snapshot",
            "playlist_catalog_snapshot",
            "playlist_item_snapshot",
            "user_category_interest",
            "user_author_interest",
            "user_podcast_interaction",
            "podcast_daily_stats",
            "author_daily_stats",
            "playlist_daily_stats",
            "recommendation_cache",
            "global_recommendation_cache"
    );

    private static final Set<String> EXPECTED_INDEXES = Set.of(
            "idx_processed_events_processed_at",
            "idx_podcast_catalog_snapshot_author_id",
            "idx_podcast_catalog_snapshot_category_id",
            "idx_playlist_item_snapshot_playlist_position",
            "idx_user_category_interest_user_score",
            "idx_user_author_interest_user_score",
            "idx_user_podcast_interaction_user_podcast",
            "idx_podcast_daily_stats_stat_date",
            "idx_author_daily_stats_stat_date",
            "idx_playlist_daily_stats_stat_date",
            "idx_recommendation_cache_expires_at",
            "idx_recommendation_cache_user_type_rank",
            "idx_global_recommendation_cache_expires_at",
            "idx_global_recommendation_cache_type_rank"
    );

    @Autowired
    private DataSource dataSource;

    @Test
    void flywayCreatesRecommendationReadModelSchema() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            assertThat(readTableNames(metaData)).containsAll(EXPECTED_TABLES);
            assertThat(readIndexNames(metaData)).containsAll(EXPECTED_INDEXES);
        }
    }

    private Set<String> readTableNames(DatabaseMetaData metaData) throws SQLException {
        Set<String> tableNames = new HashSet<>();
        try (ResultSet resultSet = metaData.getTables(null, null, null, new String[]{"TABLE"})) {
            while (resultSet.next()) {
                tableNames.add(normalize(resultSet.getString("TABLE_NAME")));
            }
        }
        return tableNames;
    }

    private Set<String> readIndexNames(DatabaseMetaData metaData) throws SQLException {
        Set<String> indexNames = new HashSet<>();
        for (String tableName : EXPECTED_TABLES) {
            try (ResultSet resultSet = metaData.getIndexInfo(null, null, tableName, false, false)) {
                while (resultSet.next()) {
                    String indexName = resultSet.getString("INDEX_NAME");
                    if (indexName != null) {
                        indexNames.add(normalize(indexName));
                    }
                }
            }
        }
        return indexNames;
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
