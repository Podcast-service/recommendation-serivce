package recommendationService.events.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PodcastDislikedPayload(
        @NotNull UUID podcastId,
        @NotNull UUID userId,
        @NotNull Instant dislikedAt,
        UUID authorId,
        UUID categoryId
) {
}
