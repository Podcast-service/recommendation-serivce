package recommendationService.events.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlaylistCreatedPayload(
        @NotNull UUID playlistId,
        @NotNull UUID ownerUserId,
        @NotBlank String title,
        boolean publicPlaylist,
        @NotNull Instant createdAt
) {
}
