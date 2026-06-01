package recommendationService.events;

import java.util.Arrays;
import java.util.Optional;
import recommendationService.events.payload.AuthorFollowedPayload;
import recommendationService.events.payload.AuthorUnfollowedPayload;
import recommendationService.events.payload.PlaylistCreatedPayload;
import recommendationService.events.payload.PlaylistDeletedPayload;
import recommendationService.events.payload.PlaylistUpdatedPayload;
import recommendationService.events.payload.PodcastDeletedPayload;
import recommendationService.events.payload.PodcastDislikedPayload;
import recommendationService.events.payload.PodcastLikedPayload;
import recommendationService.events.payload.PodcastPlayFinishedPayload;
import recommendationService.events.payload.PodcastPublishedPayload;
import recommendationService.events.payload.PodcastUpdatedPayload;

public enum RecommendationEventType {

    PODCAST_PUBLISHED("podcast.published.v1", PodcastPublishedPayload.class),
    PODCAST_UPDATED("podcast.updated.v1", PodcastUpdatedPayload.class),
    PODCAST_DELETED("podcast.deleted.v1", PodcastDeletedPayload.class),
    PODCAST_PLAY_FINISHED("podcast.play_finished.v1", PodcastPlayFinishedPayload.class),
    PODCAST_LIKED("podcast.liked.v1", PodcastLikedPayload.class),
    PODCAST_DISLIKED("podcast.disliked.v1", PodcastDislikedPayload.class),
    AUTHOR_FOLLOWED("author.followed.v1", AuthorFollowedPayload.class),
    AUTHOR_UNFOLLOWED("author.unfollowed.v1", AuthorUnfollowedPayload.class),
    PLAYLIST_CREATED("playlist.created.v1", PlaylistCreatedPayload.class),
    PLAYLIST_UPDATED("playlist.updated.v1", PlaylistUpdatedPayload.class),
    PLAYLIST_DELETED("playlist.deleted.v1", PlaylistDeletedPayload.class);

    private final String value;
    private final Class<?> payloadType;

    RecommendationEventType(String value, Class<?> payloadType) {
        this.value = value;
        this.payloadType = payloadType;
    }

    public String value() {
        return value;
    }

    public Class<?> payloadType() {
        return payloadType;
    }

    public static Optional<RecommendationEventType> fromValue(String value) {
        return Arrays.stream(values())
                .filter(type -> type.value.equals(value))
                .findFirst();
    }
}
