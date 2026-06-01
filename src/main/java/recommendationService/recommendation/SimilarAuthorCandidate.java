package recommendationService.recommendation;

import java.math.BigDecimal;

record SimilarAuthorCandidate(
        String authorId,
        String displayName,
        BigDecimal categoryScore,
        BigDecimal audienceScore,
        BigDecimal trendScore
) {
}
