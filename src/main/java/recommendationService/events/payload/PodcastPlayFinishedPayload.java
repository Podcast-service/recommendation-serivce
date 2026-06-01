package recommendationService.events.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PodcastPlayFinishedPayload(
        @NotNull UUID podcastId,
        @NotNull UUID userId,
        UUID authorId,
        UUID categoryId,
        Long durationSeconds,
        @JsonAlias("listenedSeconds") @PositiveOrZero long progressSeconds,
        String source,
        Instant occurredAt,
        Instant finishedAt,
        BigDecimal progressPercent
) {
}
