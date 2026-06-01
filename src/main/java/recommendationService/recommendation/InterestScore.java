package recommendationService.recommendation;

import java.math.BigDecimal;

record InterestScore(
        String itemId,
        BigDecimal score
) {
}
