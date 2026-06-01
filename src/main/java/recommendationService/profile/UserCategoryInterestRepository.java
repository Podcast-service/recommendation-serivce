package recommendationService.profile;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCategoryInterestRepository extends JpaRepository<UserCategoryInterestEntity, UserCategoryInterestId> {
}
