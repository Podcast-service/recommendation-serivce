package recommendationService.recommendation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recommendation/v1")
@ConditionalOnProperty(prefix = "app.features", name = "recommendation-blocks-api-enabled", havingValue = "true", matchIfMissing = true)
@Tag(name = "Recommendation Blocks")
public class RecommendationBlocksController {

    private final RecommendationBlocksService service;

    public RecommendationBlocksController(RecommendationBlocksService service) {
        this.service = service;
    }

    @GetMapping("/feed")
    @Operation(summary = "Get mixed personal recommendation feed")
    public ResponseEntity<List<RecommendationItemResponse>> feed(
            @Parameter(description = "User identifier")
            @RequestParam String userId,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(service.feed(userId, limit));
    }

    @GetMapping("/playlists")
    @Operation(summary = "Get personal playlist recommendations")
    public ResponseEntity<List<RecommendationItemResponse>> playlists(
            @Parameter(description = "User identifier")
            @RequestParam String userId,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(service.playlists(userId, limit));
    }

    @GetMapping("/podcasts/{podcastId}/similar")
    @Operation(summary = "Get podcasts similar to the source podcast")
    public ResponseEntity<List<RecommendationItemResponse>> similarPodcasts(
            @PathVariable String podcastId,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(service.similarPodcasts(podcastId, limit));
    }

    @GetMapping("/authors/{authorId}/similar")
    @Operation(summary = "Get authors similar to the source author")
    public ResponseEntity<List<RecommendationItemResponse>> similarAuthors(
            @PathVariable String authorId,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(service.similarAuthors(authorId, limit));
    }
}
