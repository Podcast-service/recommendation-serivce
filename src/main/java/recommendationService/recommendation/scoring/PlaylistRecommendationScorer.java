package recommendationService.recommendation.scoring;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class PlaylistRecommendationScorer {

    public BigDecimal score(
            BigDecimal category,
            BigDecimal author,
            BigDecimal popularity,
            BigDecimal quality,
            BigDecimal freshness
    ) {
        return category.multiply(new BigDecimal("0.35"))
                .add(author.multiply(new BigDecimal("0.25")))
                .add(popularity.multiply(new BigDecimal("0.20")))
                .add(quality.multiply(new BigDecimal("0.10")))
                .add(freshness.multiply(new BigDecimal("0.10")))
                .max(BigDecimal.ZERO)
                .min(BigDecimal.ONE)
                .multiply(new BigDecimal("100"))
                .setScale(4, RoundingMode.HALF_UP);
    }
}
