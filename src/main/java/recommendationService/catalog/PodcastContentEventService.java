package recommendationService.catalog;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import recommendationService.events.DomainEventEnvelope;
import recommendationService.events.ParsedDomainEvent;
import recommendationService.events.ProcessedEventEntity;
import recommendationService.events.ProcessedEventRepository;
import recommendationService.events.RecommendationEventMapper;
import recommendationService.events.RecommendationEventType;
import recommendationService.events.payload.PlaylistCreatedPayload;
import recommendationService.events.payload.PlaylistDeletedPayload;
import recommendationService.events.payload.PlaylistUpdatedPayload;
import recommendationService.events.payload.PodcastDeletedPayload;
import recommendationService.events.payload.PodcastPublishedPayload;
import recommendationService.events.payload.PodcastUpdatedPayload;

@Service
public class PodcastContentEventService {

    private static final Logger log = LoggerFactory.getLogger(PodcastContentEventService.class);

    private static final Set<RecommendationEventType> CONTENT_EVENT_TYPES = Set.of(
            RecommendationEventType.PODCAST_PUBLISHED,
            RecommendationEventType.PODCAST_UPDATED,
            RecommendationEventType.PODCAST_DELETED,
            RecommendationEventType.PLAYLIST_CREATED,
            RecommendationEventType.PLAYLIST_UPDATED,
            RecommendationEventType.PLAYLIST_DELETED
    );

    private final RecommendationEventMapper eventMapper;
    private final ProcessedEventRepository processedEventRepository;
    private final PodcastCatalogSnapshotRepository podcastRepository;
    private final PlaylistCatalogSnapshotRepository playlistRepository;
    private final PlaylistItemSnapshotRepository playlistItemRepository;
    private final MeterRegistry meterRegistry;

    public PodcastContentEventService(
            RecommendationEventMapper eventMapper,
            ProcessedEventRepository processedEventRepository,
            PodcastCatalogSnapshotRepository podcastRepository,
            PlaylistCatalogSnapshotRepository playlistRepository,
            PlaylistItemSnapshotRepository playlistItemRepository,
            MeterRegistry meterRegistry
    ) {
        this.eventMapper = eventMapper;
        this.processedEventRepository = processedEventRepository;
        this.podcastRepository = podcastRepository;
        this.playlistRepository = playlistRepository;
        this.playlistItemRepository = playlistItemRepository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public ContentEventHandlingResult handle(String rawEventJson) {
        ParsedDomainEvent parsedEvent = eventMapper.read(rawEventJson);
        DomainEventEnvelope<?> envelope = parsedEvent.envelope();
        String eventId = envelope.eventId().toString();

        if (processedEventRepository.existsById(eventId)) {
            meterRegistry.counter("recommendation.events.duplicates").increment();
            log.info("recommendation_content_event_duplicate eventId={} eventType={}", eventId, envelope.eventType());
            return ContentEventHandlingResult.DUPLICATE;
        }

        if (!parsedEvent.knownEventType() || !CONTENT_EVENT_TYPES.contains(parsedEvent.eventType())) {
            saveProcessedEvent(envelope);
            log.info("recommendation_content_event_ignored eventId={} eventType={}", eventId, envelope.eventType());
            return ContentEventHandlingResult.IGNORED;
        }

        applyContentEvent(parsedEvent.eventType(), envelope);
        saveProcessedEvent(envelope);
        meterRegistry.counter("recommendation.events.processed").increment();
        log.info("recommendation_content_event_processed eventId={} eventType={}", eventId, envelope.eventType());
        return ContentEventHandlingResult.PROCESSED;
    }

    private void applyContentEvent(
            RecommendationEventType eventType,
            DomainEventEnvelope<?> envelope
    ) {
        switch (eventType) {
            case PODCAST_PUBLISHED -> upsertPodcast((PodcastPublishedPayload) envelope.payload(), envelope.occurredAt());
            case PODCAST_UPDATED -> upsertPodcast((PodcastUpdatedPayload) envelope.payload(), envelope.occurredAt());
            case PODCAST_DELETED -> markPodcastDeleted((PodcastDeletedPayload) envelope.payload(), envelope.occurredAt());
            case PLAYLIST_CREATED -> upsertPlaylist((PlaylistCreatedPayload) envelope.payload(), envelope.occurredAt());
            case PLAYLIST_UPDATED -> upsertPlaylist((PlaylistUpdatedPayload) envelope.payload(), envelope.occurredAt());
            case PLAYLIST_DELETED -> markPlaylistDeleted((PlaylistDeletedPayload) envelope.payload(), envelope.occurredAt());
            default -> throw new IllegalArgumentException("Unsupported content event type: " + eventType.value());
        }
    }

    private void upsertPodcast(PodcastPublishedPayload payload, Instant eventTime) {
        PodcastCatalogSnapshotEntity snapshot = podcastRepository.findById(payload.podcastId().toString())
                .orElseGet(() -> new PodcastCatalogSnapshotEntity(payload.podcastId().toString(), eventTime));
        snapshot.setAuthorId(payload.authorId().toString());
        snapshot.setCategoryId(payload.categoryId().toString());
        snapshot.setTitle(payload.title());
        snapshot.setStatus(CatalogSnapshotStatus.PUBLISHED);
        snapshot.setPublishedAt(payload.publishedAt());
        snapshot.setUpdatedAt(eventTime);
        podcastRepository.save(snapshot);
    }

    private void upsertPodcast(PodcastUpdatedPayload payload, Instant eventTime) {
        PodcastCatalogSnapshotEntity snapshot = podcastRepository.findById(payload.podcastId().toString())
                .orElseGet(() -> new PodcastCatalogSnapshotEntity(payload.podcastId().toString(), eventTime));
        snapshot.setAuthorId(payload.authorId().toString());
        snapshot.setCategoryId(payload.categoryId().toString());
        snapshot.setTitle(payload.title());
        snapshot.setStatus(CatalogSnapshotStatus.PUBLISHED);
        snapshot.setUpdatedAt(eventTime);
        podcastRepository.save(snapshot);
    }

    private void markPodcastDeleted(PodcastDeletedPayload payload, Instant eventTime) {
        // Tombstone rows keep delete events visible even if Kafka delivery is out of order.
        PodcastCatalogSnapshotEntity snapshot = podcastRepository.findById(payload.podcastId().toString())
                .orElseGet(() -> new PodcastCatalogSnapshotEntity(payload.podcastId().toString(), eventTime));
        snapshot.setAuthorId(payload.authorId().toString());
        snapshot.setStatus(CatalogSnapshotStatus.DELETED);
        snapshot.setUpdatedAt(eventTime);
        podcastRepository.save(snapshot);
    }

    private void upsertPlaylist(PlaylistCreatedPayload payload, Instant eventTime) {
        PlaylistCatalogSnapshotEntity snapshot = playlistRepository.findById(payload.playlistId().toString())
                .orElseGet(() -> new PlaylistCatalogSnapshotEntity(payload.playlistId().toString(), eventTime));
        snapshot.setOwnerUserId(payload.ownerUserId().toString());
        snapshot.setTitle(payload.title());
        snapshot.setVisibility(visibility(payload.publicPlaylist()));
        snapshot.setStatus(CatalogSnapshotStatus.ACTIVE);
        snapshot.setUpdatedAt(eventTime);
        playlistRepository.save(snapshot);
        replacePlaylistItemsIfPresent(payload.playlistId(), payload.podcastIds(), eventTime);
    }

    private void upsertPlaylist(PlaylistUpdatedPayload payload, Instant eventTime) {
        PlaylistCatalogSnapshotEntity snapshot = playlistRepository.findById(payload.playlistId().toString())
                .orElseGet(() -> new PlaylistCatalogSnapshotEntity(payload.playlistId().toString(), eventTime));
        snapshot.setOwnerUserId(payload.ownerUserId().toString());
        snapshot.setTitle(payload.title());
        snapshot.setVisibility(visibility(payload.publicPlaylist()));
        snapshot.setStatus(CatalogSnapshotStatus.ACTIVE);
        snapshot.setUpdatedAt(eventTime);
        playlistRepository.save(snapshot);
        replacePlaylistItemsIfPresent(payload.playlistId(), payload.podcastIds(), eventTime);
    }

    private void markPlaylistDeleted(PlaylistDeletedPayload payload, Instant eventTime) {
        // Tombstone rows keep delete events visible even if Kafka delivery is out of order.
        PlaylistCatalogSnapshotEntity snapshot = playlistRepository.findById(payload.playlistId().toString())
                .orElseGet(() -> new PlaylistCatalogSnapshotEntity(payload.playlistId().toString(), eventTime));
        snapshot.setOwnerUserId(payload.ownerUserId().toString());
        snapshot.setStatus(CatalogSnapshotStatus.DELETED);
        snapshot.setUpdatedAt(eventTime);
        playlistRepository.save(snapshot);
    }

    private void replacePlaylistItemsIfPresent(UUID playlistId, List<UUID> podcastIds, Instant eventTime) {
        if (podcastIds == null) {
            return;
        }

        String playlistIdValue = playlistId.toString();
        playlistItemRepository.deleteById_PlaylistId(playlistIdValue);
        for (int index = 0; index < podcastIds.size(); index++) {
            playlistItemRepository.save(new PlaylistItemSnapshotEntity(
                    playlistIdValue,
                    podcastIds.get(index).toString(),
                    index,
                    eventTime
            ));
        }
    }

    private void saveProcessedEvent(DomainEventEnvelope<?> envelope) {
        processedEventRepository.save(new ProcessedEventEntity(
                envelope.eventId().toString(),
                envelope.eventType(),
                envelope.eventVersion(),
                Instant.now()
        ));
    }

    private String visibility(boolean publicPlaylist) {
        return publicPlaylist ? CatalogVisibility.PUBLIC : CatalogVisibility.PRIVATE;
    }
}
