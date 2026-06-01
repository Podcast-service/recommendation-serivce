package recommendationService.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "user_podcast_interaction")
public class UserPodcastInteractionEntity {

    @Id
    @Column(name = "interaction_id", nullable = false, length = 128)
    private String interactionId;

    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    @Column(name = "podcast_id", nullable = false, length = 128)
    private String podcastId;

    @Column(name = "interaction_type", nullable = false, length = 64)
    private String interactionType;

    @Column(name = "interaction_value", precision = 12, scale = 6)
    private BigDecimal interactionValue;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "play_finished_count", nullable = false)
    private long playFinishedCount;

    @Column(name = "liked", nullable = false)
    private boolean liked;

    @Column(name = "disliked", nullable = false)
    private boolean disliked;

    @Column(name = "max_progress_percent", precision = 5, scale = 2)
    private BigDecimal maxProgressPercent;

    @Column(name = "last_interaction_at")
    private Instant lastInteractionAt;

    protected UserPodcastInteractionEntity() {
    }

    public UserPodcastInteractionEntity(String userId, String podcastId, Instant eventTime) {
        this.interactionId = userId + ":" + podcastId;
        this.userId = userId;
        this.podcastId = podcastId;
        this.interactionType = "ACTIVITY";
        this.occurredAt = eventTime;
        this.createdAt = eventTime;
    }

    public long getPlayFinishedCount() {
        return playFinishedCount;
    }

    public void incrementPlayFinishedCount() {
        this.playFinishedCount++;
    }

    public boolean isLiked() {
        return liked;
    }

    public void markLiked() {
        this.liked = true;
        this.disliked = false;
        this.interactionType = "LIKED";
    }

    public boolean isDisliked() {
        return disliked;
    }

    public void markDisliked() {
        this.disliked = true;
        this.liked = false;
        this.interactionType = "DISLIKED";
    }

    public BigDecimal getMaxProgressPercent() {
        return maxProgressPercent;
    }

    public void updateMaxProgressPercent(BigDecimal progressPercent) {
        if (progressPercent == null) {
            return;
        }
        if (maxProgressPercent == null || progressPercent.compareTo(maxProgressPercent) > 0) {
            this.maxProgressPercent = progressPercent;
        }
        this.interactionValue = this.maxProgressPercent;
    }

    public Instant getLastInteractionAt() {
        return lastInteractionAt;
    }

    public void touch(Instant eventTime) {
        this.lastInteractionAt = eventTime;
        this.occurredAt = eventTime;
    }
}
