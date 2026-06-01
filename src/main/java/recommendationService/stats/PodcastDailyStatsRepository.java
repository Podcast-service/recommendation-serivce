package recommendationService.stats;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PodcastDailyStatsRepository extends JpaRepository<PodcastDailyStatsEntity, PodcastDailyStatsId> {
}
