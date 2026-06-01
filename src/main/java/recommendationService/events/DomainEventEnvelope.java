package recommendationService.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DomainEventEnvelope<T>(
        UUID eventId,
        String eventType,
        int eventVersion,
        String producer,
        Instant occurredAt,
        String correlationId,
        String causationId,
        UUID userId,
        T payload
) {
}
