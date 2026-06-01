package recommendationService.trends;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recommendation/v1/trends")
@ConditionalOnProperty(prefix = "app.features", name = "trends-api-enabled", havingValue = "true", matchIfMissing = true)
@Tag(name = "Trends")
public class TrendController {

    private final TrendService trendService;

    public TrendController(TrendService trendService) {
        this.trendService = trendService;
    }

    @GetMapping("/podcasts")
    @Operation(summary = "Get podcast trends")
    public ResponseEntity<List<TrendItemResponse>> podcastTrends(
            @Parameter(description = "Aggregation period: day, week, month")
            @RequestParam(defaultValue = "day") String period,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(trendService.podcastTrends(parsePeriod(period), categoryId, limit));
    }

    @GetMapping("/authors")
    @Operation(summary = "Get author trends")
    public ResponseEntity<List<TrendItemResponse>> authorTrends(
            @Parameter(description = "Aggregation period: day, week, month")
            @RequestParam(defaultValue = "day") String period,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(trendService.authorTrends(parsePeriod(period), limit));
    }

    @GetMapping("/playlists")
    @Operation(summary = "Get playlist trends")
    public ResponseEntity<List<TrendItemResponse>> playlistTrends(
            @Parameter(description = "Aggregation period: day, week, month")
            @RequestParam(defaultValue = "day") String period,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(trendService.playlistTrends(parsePeriod(period), limit));
    }

    private TrendPeriod parsePeriod(String value) {
        try {
            return TrendPeriod.valueOf(value.trim().toUpperCase());
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("period must be one of: day, week, month");
        }
    }
}
