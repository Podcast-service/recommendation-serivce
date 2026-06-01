package recommendationService.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistItemSnapshotRepository extends JpaRepository<PlaylistItemSnapshotEntity, PlaylistItemSnapshotId> {

    void deleteById_PlaylistId(String playlistId);

    long countById_PlaylistId(String playlistId);
}
