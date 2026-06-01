package recommendationService.stats;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorDailyStatsRepository extends JpaRepository<AuthorDailyStatsEntity, AuthorDailyStatsId> {
}
