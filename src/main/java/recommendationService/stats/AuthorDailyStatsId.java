package recommendationService.stats;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class AuthorDailyStatsId implements Serializable {

    @Column(name = "author_id", nullable = false, length = 128)
    private String authorId;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    protected AuthorDailyStatsId() {
    }

    public AuthorDailyStatsId(String authorId, LocalDate statDate) {
        this.authorId = authorId;
        this.statDate = statDate;
    }

    public String getAuthorId() {
        return authorId;
    }

    public LocalDate getStatDate() {
        return statDate;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AuthorDailyStatsId that)) {
            return false;
        }
        return Objects.equals(authorId, that.authorId)
                && Objects.equals(statDate, that.statDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authorId, statDate);
    }
}
