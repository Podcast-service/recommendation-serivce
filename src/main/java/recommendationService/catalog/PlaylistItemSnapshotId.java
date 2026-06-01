package recommendationService.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class PlaylistItemSnapshotId implements Serializable {

    @Column(name = "playlist_id", nullable = false, length = 128)
    private String playlistId;

    @Column(name = "podcast_id", nullable = false, length = 128)
    private String podcastId;

    protected PlaylistItemSnapshotId() {
    }

    public PlaylistItemSnapshotId(String playlistId, String podcastId) {
        this.playlistId = playlistId;
        this.podcastId = podcastId;
    }

    public String getPlaylistId() {
        return playlistId;
    }

    public String getPodcastId() {
        return podcastId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PlaylistItemSnapshotId that)) {
            return false;
        }
        return Objects.equals(playlistId, that.playlistId)
                && Objects.equals(podcastId, that.podcastId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playlistId, podcastId);
    }
}
