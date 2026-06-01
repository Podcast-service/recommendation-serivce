package recommendationService.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistCatalogSnapshotRepository extends JpaRepository<PlaylistCatalogSnapshotEntity, String> {
}
