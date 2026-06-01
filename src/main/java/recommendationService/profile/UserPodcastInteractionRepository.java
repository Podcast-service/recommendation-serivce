package recommendationService.profile;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPodcastInteractionRepository extends JpaRepository<UserPodcastInteractionEntity, String> {
}
