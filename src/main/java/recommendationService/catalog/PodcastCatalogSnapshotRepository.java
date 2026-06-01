package recommendationService.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PodcastCatalogSnapshotRepository extends JpaRepository<PodcastCatalogSnapshotEntity, String> {
}
