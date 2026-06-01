package recommendationService.recommendation;

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
@RequestMapping("/recommendation/v1")
@ConditionalOnProperty(prefix = "app.features", name = "personal-podcasts-api-enabled", havingValue = "true", matchIfMissing = true)
@Tag(name = "Personal Recommendations")
public class PodcastRecommendationController {

    private final PodcastRecommendationService service;

    public PodcastRecommendationController(PodcastRecommendationService service) {
        this.service = service;
    }

    @GetMapping("/podcasts")
    @Operation(summary = "Get personal podcast recommendations")
    public ResponseEntity<List<PodcastRecommendationResponse>> recommendPodcasts(
            @Parameter(description = "User identifier")
            @RequestParam String userId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String categoryId,
            @RequestParam(defaultValue = "true") Boolean excludeSeen
    ) {
        return ResponseEntity.ok(service.recommendPodcasts(userId, limit, categoryId, excludeSeen));
    }
}
