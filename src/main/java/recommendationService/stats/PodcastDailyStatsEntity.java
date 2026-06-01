package recommendationService.stats;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "podcast_daily_stats")
public class PodcastDailyStatsEntity {

    @EmbeddedId
    private PodcastDailyStatsId id;

    @Column(name = "play_count", nullable = false)
    private long playCount;

    @Column(name = "completion_count", nullable = false)
    private long completionCount;

    @Column(name = "like_count", nullable = false)
    private long likeCount;

    @Column(name = "dislike_count", nullable = false)
    private long dislikeCount;

    @Column(name = "play_finished_count", nullable = false)
    private long playFinishedCount;

    @Column(name = "share_count", nullable = false)
    private long shareCount;

    @Column(name = "rating_count", nullable = false)
    private long ratingCount;

    @Column(name = "rating_sum", nullable = false, precision = 14, scale = 4)
    private BigDecimal ratingSum;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PodcastDailyStatsEntity() {
    }

    public PodcastDailyStatsEntity(String podcastId, LocalDate statDate, Instant now) {
        this.id = new PodcastDailyStatsId(podcastId, statDate);
        this.ratingSum = BigDecimal.ZERO;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public long getLikeCount() {
        return likeCount;
    }

    public long getDislikeCount() {
        return dislikeCount;
    }

    public long getPlayFinishedCount() {
        return playFinishedCount;
    }

    public void incrementLike(Instant eventTime) {
        this.likeCount++;
        this.updatedAt = eventTime;
    }

    public void incrementDislike(Instant eventTime) {
        this.dislikeCount++;
        this.updatedAt = eventTime;
    }

    public void incrementPlayFinished(Instant eventTime) {
        this.playFinishedCount++;
        this.playCount++;
        this.completionCount++;
        this.updatedAt = eventTime;
    }
}
