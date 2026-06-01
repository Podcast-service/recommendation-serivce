package recommendationService.events;

import java.time.Instant;
import java.sql.Timestamp;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProcessedEventWriter {

    private final JdbcTemplate jdbcTemplate;
    private final boolean postgres;

    public ProcessedEventWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.postgres = jdbcTemplate.execute((ConnectionCallback<Boolean>) connection ->
                "PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName())
        );
    }

    public int insertIfAbsent(String eventId, String eventType, int eventVersion, Instant processedAt) {
        if (postgres) {
            return jdbcTemplate.update("""
                    insert into processed_events(event_id, event_type, event_version, processed_at)
                    values (?, ?, ?, ?)
                    on conflict (event_id) do nothing
                    """, eventId, eventType, eventVersion, Timestamp.from(processedAt));
        }

        // H2 compatibility for repository tests. Production always uses the atomic PostgreSQL statement above.
        return jdbcTemplate.update("""
                insert into processed_events(event_id, event_type, event_version, processed_at)
                select ?, ?, ?, ?
                 where not exists (
                       select 1 from processed_events where event_id = ?
                 )
                """, eventId, eventType, eventVersion, Timestamp.from(processedAt), eventId);
    }
}
