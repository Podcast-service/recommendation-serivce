package recommendationService.recommendation;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "Ranked personal podcast recommendation")
public record PodcastRecommendationResponse(
        @Schema(description = "Podcast identifier")
        String itemId,
        @Schema(description = "One-based rank inside the response")
        int rank,
        @Schema(description = "Normalized recommendation score in 0..100 range")
        BigDecimal score,
        @Schema(description = "Machine-readable reason code")
        String reasonCode,
        @Schema(description = "Human-readable reason")
        String reasonText,
        @Schema(description = "Basic podcast snapshot metadata")
        Map<String, Object> metadata
) {
}
