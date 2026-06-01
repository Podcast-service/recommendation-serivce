package recommendationService.recommendation;

import java.math.BigDecimal;
import java.time.Instant;

record PlaylistRecommendationCandidate(
        String playlistId,
        String title,
        String ownerUserId,
        Instant updatedAt,
        BigDecimal categoryInterestScore,
        BigDecimal authorInterestScore,
        BigDecimal popularityScore,
        BigDecimal qualityScore
) {
}
