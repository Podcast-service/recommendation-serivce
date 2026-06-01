package recommendationService.stats;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "author_daily_stats")
public class AuthorDailyStatsEntity {

    @EmbeddedId
    private AuthorDailyStatsId id;

    @Column(name = "podcast_count", nullable = false)
    private long podcastCount;

    @Column(name = "play_count", nullable = false)
    private long playCount;

    @Column(name = "completion_count", nullable = false)
    private long completionCount;

    @Column(name = "follower_count", nullable = false)
    private long followerCount;

    @Column(name = "followed_count", nullable = false)
    private long followedCount;

    @Column(name = "unfollowed_count", nullable = false)
    private long unfollowedCount;

    @Column(name = "like_count", nullable = false)
    private long likeCount;

    @Column(name = "dislike_count", nullable = false)
    private long dislikeCount;

    @Column(name = "play_finished_count", nullable = false)
    private long playFinishedCount;

    @Column(name = "rating_count", nullable = false)
    private long ratingCount;

    @Column(name = "rating_sum", nullable = false, precision = 14, scale = 4)
    private BigDecimal ratingSum;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AuthorDailyStatsEntity() {
    }

    public AuthorDailyStatsEntity(String authorId, LocalDate statDate, Instant now) {
        this.id = new AuthorDailyStatsId(authorId, statDate);
        this.ratingSum = BigDecimal.ZERO;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public long getFollowedCount() {
        return followedCount;
    }

    public long getUnfollowedCount() {
        return unfollowedCount;
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

    public void incrementFollowed(Instant eventTime) {
        this.followedCount++;
        this.followerCount++;
        this.updatedAt = eventTime;
    }

    public void incrementUnfollowed(Instant eventTime) {
        this.unfollowedCount++;
        this.followerCount--;
        this.updatedAt = eventTime;
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
