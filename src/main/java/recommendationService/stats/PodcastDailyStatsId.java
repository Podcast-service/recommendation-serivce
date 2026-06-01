package recommendationService.stats;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class PodcastDailyStatsId implements Serializable {

    @Column(name = "podcast_id", nullable = false, length = 128)
    private String podcastId;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    protected PodcastDailyStatsId() {
    }

    public PodcastDailyStatsId(String podcastId, LocalDate statDate) {
        this.podcastId = podcastId;
        this.statDate = statDate;
    }

    public String getPodcastId() {
        return podcastId;
    }

    public LocalDate getStatDate() {
        return statDate;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PodcastDailyStatsId that)) {
            return false;
        }
        return Objects.equals(podcastId, that.podcastId)
                && Objects.equals(statDate, that.statDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(podcastId, statDate);
    }
}
