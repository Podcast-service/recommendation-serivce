package recommendationService.events.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthorUnfollowedPayload(
        @NotNull UUID authorId,
        @NotNull UUID userId,
        @NotNull Instant unfollowedAt
) {
}
