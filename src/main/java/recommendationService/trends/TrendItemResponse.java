package recommendationService.trends;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "Ranked trend item")
public record TrendItemResponse(
        @Schema(description = "Podcast, author, or playlist identifier")
        String itemId,
        @Schema(description = "One-based rank inside the response")
        int rank,
        @Schema(description = "Aggregated trend score")
        BigDecimal score,
        @Schema(description = "Machine-readable reason code")
        String reasonCode,
        @Schema(description = "Human-readable reason")
        String reasonText,
        @Schema(description = "Snapshot metadata when available")
        Map<String, Object> metadata
) {
}
