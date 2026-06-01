package recommendationService.profile;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "user_category_interest")
public class UserCategoryInterestEntity {

    @EmbeddedId
    private UserCategoryInterestId id;

    @Column(name = "interest_score", nullable = false, precision = 12, scale = 6)
    private BigDecimal interestScore;

    @Column(name = "signal_count", nullable = false)
    private long signalCount;

    @Column(name = "last_signal_at")
    private Instant lastSignalAt;

    @Column(name = "last_event_at")
    private Instant lastEventAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserCategoryInterestEntity() {
    }

    public UserCategoryInterestEntity(String userId, String categoryId, Instant now) {
        this.id = new UserCategoryInterestId(userId, categoryId);
        this.interestScore = BigDecimal.ZERO;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public BigDecimal getInterestScore() {
        return interestScore;
    }

    public long getSignalCount() {
        return signalCount;
    }

    public void apply(BigDecimal delta, Instant eventTime) {
        this.interestScore = this.interestScore.add(delta);
        this.signalCount++;
        this.lastSignalAt = eventTime;
        this.lastEventAt = eventTime;
        this.updatedAt = eventTime;
    }
}
