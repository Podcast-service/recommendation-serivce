package recommendationService.recommendation;

import java.math.BigDecimal;
import java.time.Instant;

record PodcastRecommendationCandidate(
        String podcastId,
        String title,
        String authorId,
        String categoryId,
        Instant publishedAt,
        BigDecimal popularityScore,
        long likeCount,
        long dislikeCount,
        long ratingCount,
        BigDecimal ratingSum,
        boolean liked,
        boolean disliked,
        long playFinishedCount,
        Instant lastInteractionAt
) {
}
