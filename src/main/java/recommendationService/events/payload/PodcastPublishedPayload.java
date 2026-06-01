package recommendationService.events.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PodcastPublishedPayload(
        @NotNull UUID podcastId,
        @NotNull UUID authorId,
        @NotNull UUID categoryId,
        @NotBlank String title,
        @NotNull Instant publishedAt
) {
}
