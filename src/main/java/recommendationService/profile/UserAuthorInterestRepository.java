package recommendationService.profile;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAuthorInterestRepository extends JpaRepository<UserAuthorInterestEntity, UserAuthorInterestId> {
}
