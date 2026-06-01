package recommendationService.recommendation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recommendation/v1")
@ConditionalOnProperty(prefix = "app.features", name = "personal-podcasts-api-enabled", havingValue = "true", matchIfMissing = true)
@Tag(name = "Personal Recommendations")
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "Recommendations returned"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "403", description = "Foreign userId without ADMIN role"),
        @ApiResponse(responseCode = "500", description = "Unexpected server error")
})
public class PodcastRecommendationController {

    private final PodcastRecommendationService service;

    public PodcastRecommendationController(PodcastRecommendationService service) {
        this.service = service;
    }

    @GetMapping("/podcasts")
    @PreAuthorize("@recommendationUserAccess.canAccessUser(#userId, authentication)")
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
