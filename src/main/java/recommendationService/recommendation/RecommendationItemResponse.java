package recommendationService.recommendation;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "Ranked recommendation item for mixed recommendation blocks")
public record RecommendationItemResponse(
        @Schema(description = "Recommendation item type, for example PODCAST, PLAYLIST, or AUTHOR")
        String itemType,
        @Schema(description = "Item identifier")
        String itemId,
        @Schema(description = "One-based rank inside the response")
        int rank,
        @Schema(description = "Normalized recommendation score in 0..100 range")
        BigDecimal score,
        @Schema(description = "Machine-readable reason code")
        String reasonCode,
        @Schema(description = "Human-readable reason")
        String reasonText,
        @Schema(description = "Basic snapshot metadata")
        Map<String, Object> metadata
) {
}
