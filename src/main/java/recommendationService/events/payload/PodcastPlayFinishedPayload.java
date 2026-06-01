package recommendationService.events.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PodcastPlayFinishedPayload(
        @NotNull UUID podcastId,
        @NotNull UUID userId,
        @PositiveOrZero long progressSeconds,
        @NotNull Instant finishedAt,
        UUID authorId,
        UUID categoryId,
        BigDecimal progressPercent
) {
}
