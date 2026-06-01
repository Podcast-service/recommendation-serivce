package recommendationService.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "podcast_catalog_snapshot")
public class PodcastCatalogSnapshotEntity {

    @Id
    @Column(name = "podcast_id", nullable = false, length = 128)
    private String podcastId;

    @Column(name = "author_id", length = 128)
    private String authorId;

    @Column(name = "category_id", length = 128)
    private String categoryId;

    @Column(name = "title", nullable = false, length = 512)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "language", length = 32)
    private String language;

    @Column(name = "tags")
    private String tags;

    @Column(name = "is_explicit")
    private Boolean explicit;

    @Column(name = "status", nullable = false, length = 64)
    private String status;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PodcastCatalogSnapshotEntity() {
    }

    public PodcastCatalogSnapshotEntity(String podcastId, Instant now) {
        this.podcastId = podcastId;
        this.title = "";
        this.status = CatalogSnapshotStatus.ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getPodcastId() {
        return podcastId;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDurationSeconds(Long durationSeconds) {
        this.durationSeconds = durationSeconds == null ? null : Math.toIntExact(durationSeconds);
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public void setExplicit(Boolean explicit) {
        this.explicit = explicit;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
