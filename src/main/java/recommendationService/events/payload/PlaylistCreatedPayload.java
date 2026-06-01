package recommendationService.events.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlaylistCreatedPayload(
        @NotNull UUID playlistId,
        @NotNull UUID ownerUserId,
        @NotBlank String title,
        String description,
        @JsonAlias("isPublic") boolean publicPlaylist,
        List<UUID> podcastIds,
        @NotNull Instant createdAt,
        String visibility
) {
}
