package recommendationService.recommendation.scoring;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class PodcastRecommendationScorer {

    public BigDecimal score(
            BigDecimal category,
            BigDecimal author,
            BigDecimal popularity,
            BigDecimal freshness,
            BigDecimal quality,
            BigDecimal diversity,
            boolean seen,
            boolean excludeSeen,
            boolean disliked
    ) {
        BigDecimal weighted = category.multiply(new BigDecimal("0.35"))
                .add(author.multiply(new BigDecimal("0.25")))
                .add(popularity.multiply(new BigDecimal("0.20")))
                .add(freshness.multiply(new BigDecimal("0.10")))
                .add(quality.multiply(new BigDecimal("0.05")))
                .add(diversity.multiply(new BigDecimal("0.05")));
        BigDecimal seenPenalty = !excludeSeen && seen ? new BigDecimal("0.25") : BigDecimal.ZERO;
        BigDecimal dislikedPenalty = disliked ? BigDecimal.ONE : BigDecimal.ZERO;
        return percent(weighted.subtract(seenPenalty).subtract(dislikedPenalty));
    }

    private BigDecimal percent(BigDecimal value) {
        return value.max(BigDecimal.ZERO)
                .min(BigDecimal.ONE)
                .multiply(new BigDecimal("100"))
                .setScale(4, RoundingMode.HALF_UP);
    }
}
