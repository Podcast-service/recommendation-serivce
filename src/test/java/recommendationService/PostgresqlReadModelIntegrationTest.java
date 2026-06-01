package recommendationService;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import recommendationService.events.ProcessedEventWriter;

@Testcontainers(disabledWithoutDocker = true)
class PostgresqlReadModelIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("recommendation_db")
            .withUsername("recommendation_user")
            .withPassword("recommendation_pass");

    @Test
    void flywayMigrationsAndConcurrentInsertFirstWorkOnPostgresql() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        );
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        ProcessedEventWriter writer = new ProcessedEventWriter(jdbcTemplate);
        Instant now = Instant.parse("2026-06-02T00:00:00Z");
        Callable<Integer> insert = () -> writer.insertIfAbsent("event-1", "podcast.liked.v1", 1, now);

        try (var executor = Executors.newFixedThreadPool(2)) {
            int inserted = executor.invokeAll(List.of(insert, insert)).stream()
                    .mapToInt(future -> {
                        try {
                            return future.get();
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .sum();
            assertThat(inserted).isEqualTo(1);
        }
        assertThat(jdbcTemplate.queryForObject("select count(*) from processed_events", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from information_schema.columns
                 where table_name = 'podcast_catalog_snapshot'
                   and column_name = 'is_explicit'
                """, Integer.class)).isEqualTo(1);
    }
}
