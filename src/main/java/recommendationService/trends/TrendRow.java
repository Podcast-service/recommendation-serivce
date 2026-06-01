package recommendationService.trends;

import java.math.BigDecimal;
import java.util.Map;

record TrendRow(
        String itemId,
        BigDecimal score,
        String reasonCode,
        String reasonText,
        Map<String, Object> metadata
) {
}
