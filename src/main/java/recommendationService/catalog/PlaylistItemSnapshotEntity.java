package recommendationService.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "playlist_item_snapshot")
public class PlaylistItemSnapshotEntity {

    @EmbeddedId
    private PlaylistItemSnapshotId id;

    @Column(name = "item_position", nullable = false)
    private int itemPosition;

    @Column(name = "added_at")
    private Instant addedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PlaylistItemSnapshotEntity() {
    }

    public PlaylistItemSnapshotEntity(
            String playlistId,
            String podcastId,
            int itemPosition,
            Instant now
    ) {
        this.id = new PlaylistItemSnapshotId(playlistId, podcastId);
        this.itemPosition = itemPosition;
        this.addedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public PlaylistItemSnapshotId getId() {
        return id;
    }
}
