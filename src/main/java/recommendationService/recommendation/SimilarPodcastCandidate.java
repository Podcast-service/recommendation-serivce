package recommendationService.recommendation;

import java.math.BigDecimal;
import java.time.Instant;

record SimilarPodcastCandidate(
        String podcastId,
        String title,
        String authorId,
        String categoryId,
        String tags,
        Integer durationSeconds,
        Instant publishedAt,
        BigDecimal popularityScore
) {
}
