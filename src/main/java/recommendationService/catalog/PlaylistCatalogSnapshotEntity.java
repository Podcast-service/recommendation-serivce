package recommendationService.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "playlist_catalog_snapshot")
public class PlaylistCatalogSnapshotEntity {

    @Id
    @Column(name = "playlist_id", nullable = false, length = 128)
    private String playlistId;

    @Column(name = "owner_user_id", length = 128)
    private String ownerUserId;

    @Column(name = "title", nullable = false, length = 512)
    private String title;

    @Column(name = "visibility", nullable = false, length = 64)
    private String visibility;

    @Column(name = "status", nullable = false, length = 64)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PlaylistCatalogSnapshotEntity() {
    }

    public PlaylistCatalogSnapshotEntity(String playlistId, Instant now) {
        this.playlistId = playlistId;
        this.title = "";
        this.visibility = CatalogVisibility.UNKNOWN;
        this.status = CatalogSnapshotStatus.ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getPlaylistId() {
        return playlistId;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
