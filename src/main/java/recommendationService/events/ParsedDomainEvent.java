package recommendationService.events;

import com.fasterxml.jackson.databind.JsonNode;

public record ParsedDomainEvent(
        RecommendationEventType eventType,
        DomainEventEnvelope<?> envelope
) {

    public static ParsedDomainEvent known(
            RecommendationEventType eventType,
            DomainEventEnvelope<?> envelope
    ) {
        return new ParsedDomainEvent(eventType, envelope);
    }

    public static ParsedDomainEvent unknown(DomainEventEnvelope<JsonNode> envelope) {
        return new ParsedDomainEvent(null, envelope);
    }

    public boolean knownEventType() {
        return eventType != null;
    }
}
