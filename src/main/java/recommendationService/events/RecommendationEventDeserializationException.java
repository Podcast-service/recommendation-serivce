package recommendationService.events;

public class RecommendationEventDeserializationException extends RuntimeException {

    public RecommendationEventDeserializationException(String message) {
        super(message);
    }

    public RecommendationEventDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
