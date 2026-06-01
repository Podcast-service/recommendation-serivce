package recommendationService.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import recommendationService.events.ProcessedEventRepository;

@SpringBootTest
@ActiveProfiles("test")
class PodcastContentEventServiceTest {

    private static final String EVENT_ID = "01000000-0000-0000-0000-000000000001";
    private static final String EVENT_ID_2 = "01000000-0000-0000-0000-000000000002";
    private static final String EVENT_ID_3 = "01000000-0000-0000-0000-000000000003";
    private static final String PODCAST_ID = "10000000-0000-0000-0000-000000000001";
    private static final String AUTHOR_ID = "20000000-0000-0000-0000-000000000001";
    private static final String CATEGORY_ID = "30000000-0000-0000-0000-000000000001";
    private static final String CATEGORY_ID_2 = "30000000-0000-0000-0000-000000000002";
    private static final String PLAYLIST_ID = "40000000-0000-0000-0000-000000000001";
    private static final String USER_ID = "50000000-0000-0000-0000-000000000001";
    private static final String OCCURRED_AT = "2026-06-01T10:15:30Z";

    @Autowired
    private PodcastContentEventService service;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private PodcastCatalogSnapshotRepository podcastRepository;

    @Autowired
    private PlaylistCatalogSnapshotRepository playlistRepository;

    @Autowired
    private PlaylistItemSnapshotRepository playlistItemRepository;

    @BeforeEach
    void cleanDatabase() {
        processedEventRepository.deleteAll();
        playlistItemRepository.deleteAll();
        playlistRepository.deleteAll();
        podcastRepository.deleteAll();
    }

    @Test
    void contentEventCreatesSnapshot() {
        ContentEventHandlingResult result = service.handle(podcastPublished(EVENT_ID, "Initial title", CATEGORY_ID));

        Optional<PodcastCatalogSnapshotEntity> snapshot = podcastRepository.findById(PODCAST_ID);
        assertThat(result).isEqualTo(ContentEventHandlingResult.PROCESSED);
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().getTitle()).isEqualTo("Initial title");
        assertThat(snapshot.get().getStatus()).isEqualTo(CatalogSnapshotStatus.PUBLISHED);
        assertThat(processedEventRepository.existsById(EVENT_ID)).isTrue();
    }

    @Test
    void updateEventUpdatesSnapshot() {
        service.handle(podcastPublished(EVENT_ID, "Initial title", CATEGORY_ID));

        ContentEventHandlingResult result = service.handle(podcastUpdated(EVENT_ID_2, "Updated title", CATEGORY_ID_2));

        PodcastCatalogSnapshotEntity snapshot = podcastRepository.findById(PODCAST_ID).orElseThrow();
        assertThat(result).isEqualTo(ContentEventHandlingResult.PROCESSED);
        assertThat(snapshot.getTitle()).isEqualTo("Updated title");
        assertThat(snapshot.getCategoryId()).isEqualTo(CATEGORY_ID_2);
        assertThat(snapshot.getStatus()).isEqualTo(CatalogSnapshotStatus.PUBLISHED);
    }

    @Test
    void deleteEventMarksSnapshotAsDeleted() {
        service.handle(podcastPublished(EVENT_ID, "Initial title", CATEGORY_ID));

        ContentEventHandlingResult result = service.handle(podcastDeleted(EVENT_ID_2));

        PodcastCatalogSnapshotEntity snapshot = podcastRepository.findById(PODCAST_ID).orElseThrow();
        assertThat(result).isEqualTo(ContentEventHandlingResult.PROCESSED);
        assertThat(snapshot.getStatus()).isEqualTo(CatalogSnapshotStatus.DELETED);
        assertThat(snapshot.getTitle()).isEqualTo("Initial title");
    }

    @Test
    void duplicateEventDoesNotChangeDataAgain() {
        String event = playlistCreated(EVENT_ID, "Initial playlist", true, true);
        service.handle(event);

        ContentEventHandlingResult result = service.handle(playlistCreated(EVENT_ID, "Changed playlist", false, false));

        PlaylistCatalogSnapshotEntity snapshot = playlistRepository.findById(PLAYLIST_ID).orElseThrow();
        assertThat(result).isEqualTo(ContentEventHandlingResult.DUPLICATE);
        assertThat(snapshot.getTitle()).isEqualTo("Initial playlist");
        assertThat(snapshot.getVisibility()).isEqualTo(CatalogVisibility.PUBLIC);
        assertThat(playlistItemRepository.countById_PlaylistId(PLAYLIST_ID)).isEqualTo(2);
    }

    @Test
    void unknownEventTypeDoesNotBreakConsumerService() {
        ContentEventHandlingResult result = service.handle(envelope(
                EVENT_ID,
                "podcast.future_event.v1",
                """
                        {
                          "podcastId": "%s"
                        }
                        """.formatted(PODCAST_ID)
        ));

        assertThat(result).isEqualTo(ContentEventHandlingResult.IGNORED);
        assertThat(processedEventRepository.existsById(EVENT_ID)).isTrue();
        assertThat(podcastRepository.findById(PODCAST_ID)).isEmpty();
    }

    @Test
    void playlistEventReplacesItemsWhenPodcastIdsArePresent() {
        service.handle(playlistCreated(EVENT_ID, "Initial playlist", true, true));

        ContentEventHandlingResult result = service.handle(playlistUpdated(EVENT_ID_2, "Updated playlist", false));

        PlaylistCatalogSnapshotEntity snapshot = playlistRepository.findById(PLAYLIST_ID).orElseThrow();
        assertThat(result).isEqualTo(ContentEventHandlingResult.PROCESSED);
        assertThat(snapshot.getTitle()).isEqualTo("Updated playlist");
        assertThat(snapshot.getVisibility()).isEqualTo(CatalogVisibility.PRIVATE);
        assertThat(playlistItemRepository.countById_PlaylistId(PLAYLIST_ID)).isEqualTo(1);
    }

    @Test
    void playlistDeleteMarksSnapshotAsDeleted() {
        service.handle(playlistCreated(EVENT_ID, "Initial playlist", true, false));

        ContentEventHandlingResult result = service.handle(playlistDeleted(EVENT_ID_2));

        PlaylistCatalogSnapshotEntity snapshot = playlistRepository.findById(PLAYLIST_ID).orElseThrow();
        assertThat(result).isEqualTo(ContentEventHandlingResult.PROCESSED);
        assertThat(snapshot.getStatus()).isEqualTo(CatalogSnapshotStatus.DELETED);
    }

    private String podcastPublished(String eventId, String title, String categoryId) {
        return envelope(eventId, "podcast.published.v1", """
                {
                  "podcastId": "%s",
                  "authorId": "%s",
                  "categoryId": "%s",
                  "title": "%s",
                  "publishedAt": "%s"
                }
                """.formatted(PODCAST_ID, AUTHOR_ID, categoryId, title, OCCURRED_AT));
    }

    private String podcastUpdated(String eventId, String title, String categoryId) {
        return envelope(eventId, "podcast.updated.v1", """
                {
                  "podcastId": "%s",
                  "authorId": "%s",
                  "categoryId": "%s",
                  "title": "%s",
                  "updatedAt": "%s"
                }
                """.formatted(PODCAST_ID, AUTHOR_ID, categoryId, title, OCCURRED_AT));
    }

    private String podcastDeleted(String eventId) {
        return envelope(eventId, "podcast.deleted.v1", """
                {
                  "podcastId": "%s",
                  "authorId": "%s",
                  "deletedAt": "%s"
                }
                """.formatted(PODCAST_ID, AUTHOR_ID, OCCURRED_AT));
    }

    private String playlistCreated(String eventId, String title, boolean publicPlaylist, boolean withPodcastIds) {
        String podcastIds = withPodcastIds
                ? """
                  ,
                  "podcastIds": [
                    "%s",
                    "%s"
                  ]
                """.formatted(PODCAST_ID, "10000000-0000-0000-0000-000000000002")
                : "";
        return envelope(eventId, "playlist.created.v1", """
                {
                  "playlistId": "%s",
                  "ownerUserId": "%s",
                  "title": "%s",
                  "publicPlaylist": %s,
                  "createdAt": "%s"%s
                }
                """.formatted(PLAYLIST_ID, USER_ID, title, publicPlaylist, OCCURRED_AT, podcastIds));
    }

    private String playlistUpdated(String eventId, String title, boolean publicPlaylist) {
        return envelope(eventId, "playlist.updated.v1", """
                {
                  "playlistId": "%s",
                  "ownerUserId": "%s",
                  "title": "%s",
                  "publicPlaylist": %s,
                  "updatedAt": "%s",
                  "podcastIds": [
                    "%s"
                  ]
                }
                """.formatted(PLAYLIST_ID, USER_ID, title, publicPlaylist, OCCURRED_AT, PODCAST_ID));
    }

    private String playlistDeleted(String eventId) {
        return envelope(eventId, "playlist.deleted.v1", """
                {
                  "playlistId": "%s",
                  "ownerUserId": "%s",
                  "deletedAt": "%s"
                }
                """.formatted(PLAYLIST_ID, USER_ID, OCCURRED_AT));
    }

    private String envelope(String eventId, String eventType, String payload) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "%s",
                  "eventVersion": 1,
                  "producer": "podcast-core",
                  "occurredAt": "%s",
                  "correlationId": "correlation-1",
                  "causationId": "causation-1",
                  "userId": "%s",
                  "payload": %s
                }
                """.formatted(eventId, eventType, OCCURRED_AT, USER_ID, payload);
    }
}
