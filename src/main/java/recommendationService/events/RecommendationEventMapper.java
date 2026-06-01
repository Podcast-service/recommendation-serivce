package recommendationService.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RecommendationEventMapper {

    private static final TypeReference<DomainEventEnvelope<JsonNode>> RAW_ENVELOPE_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public RecommendationEventMapper(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper.copy()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.validator = validator;
    }

    public ParsedDomainEvent read(String json) {
        DomainEventEnvelope<JsonNode> rawEnvelope = readEnvelope(json);
        validateEnvelope(rawEnvelope);

        return RecommendationEventType.fromValue(rawEnvelope.eventType())
                .map(type -> {
                    if (rawEnvelope.eventVersion() != 1) {
                        throw new RecommendationEventDeserializationException(
                                "Unsupported recommendation event version: " + rawEnvelope.eventVersion()
                        );
                    }
                    return ParsedDomainEvent.known(type, mapKnownPayload(rawEnvelope, type));
                })
                .orElseGet(() -> ParsedDomainEvent.unknown(rawEnvelope));
    }

    private DomainEventEnvelope<JsonNode> readEnvelope(String json) {
        if (json == null || json.isBlank()) {
            throw new RecommendationEventDeserializationException("Recommendation event JSON must not be blank");
        }
        try {
            return objectMapper.readValue(json, RAW_ENVELOPE_TYPE);
        } catch (JsonProcessingException exception) {
            throw new RecommendationEventDeserializationException("Failed to deserialize recommendation event envelope", exception);
        }
    }

    private void validateEnvelope(DomainEventEnvelope<JsonNode> envelope) {
        if (envelope.eventId() == null) {
            throw new RecommendationEventDeserializationException("Recommendation event has missing eventId");
        }
        if (envelope.eventType() == null || envelope.eventType().isBlank()) {
            throw new RecommendationEventDeserializationException("Recommendation event has missing eventType");
        }
        if (envelope.eventVersion() <= 0) {
            throw new RecommendationEventDeserializationException("Recommendation event has invalid eventVersion");
        }
        if (envelope.producer() == null || envelope.producer().isBlank()) {
            throw new RecommendationEventDeserializationException("Recommendation event has missing producer");
        }
        if (envelope.occurredAt() == null) {
            throw new RecommendationEventDeserializationException("Recommendation event has missing occurredAt");
        }
        if (envelope.payload() == null || envelope.payload().isNull()) {
            throw new RecommendationEventDeserializationException("Recommendation event has missing payload");
        }
    }

    private DomainEventEnvelope<?> mapKnownPayload(
            DomainEventEnvelope<JsonNode> rawEnvelope,
            RecommendationEventType eventType
    ) {
        Object payload = convertPayload(rawEnvelope.payload(), eventType);
        validatePayload(payload, eventType);
        return new DomainEventEnvelope<>(
                rawEnvelope.eventId(),
                rawEnvelope.eventType(),
                rawEnvelope.eventVersion(),
                rawEnvelope.producer(),
                rawEnvelope.occurredAt(),
                rawEnvelope.correlationId(),
                rawEnvelope.causationId(),
                rawEnvelope.userId(),
                payload
        );
    }

    private Object convertPayload(JsonNode payload, RecommendationEventType eventType) {
        try {
            return objectMapper.treeToValue(payload, eventType.payloadType());
        } catch (JsonProcessingException exception) {
            throw new RecommendationEventDeserializationException(
                    "Failed to deserialize recommendation event payload: " + eventType.value(),
                    exception
            );
        }
    }

    private void validatePayload(Object payload, RecommendationEventType eventType) {
        Set<ConstraintViolation<Object>> violations = validator.validate(payload);
        if (!violations.isEmpty()) {
            throw new RecommendationEventDeserializationException(
                    "Recommendation event payload is invalid: " + eventType.value()
            );
        }
    }
}
