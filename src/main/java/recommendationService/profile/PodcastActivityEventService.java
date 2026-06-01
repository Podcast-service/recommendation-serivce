package recommendationService.profile;

import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import recommendationService.catalog.PodcastCatalogSnapshotEntity;
import recommendationService.catalog.PodcastCatalogSnapshotRepository;
import recommendationService.events.DomainEventEnvelope;
import recommendationService.events.ParsedDomainEvent;
import recommendationService.events.ProcessedEventWriter;
import recommendationService.events.RecommendationEventMapper;
import recommendationService.events.RecommendationEventDeserializationException;
import recommendationService.events.RecommendationEventType;
import recommendationService.events.payload.AuthorFollowedPayload;
import recommendationService.events.payload.AuthorUnfollowedPayload;
import recommendationService.events.payload.PodcastDislikedPayload;
import recommendationService.events.payload.PodcastLikedPayload;
import recommendationService.events.payload.PodcastPlayFinishedPayload;
import recommendationService.stats.AuthorDailyStatsEntity;
import recommendationService.stats.AuthorDailyStatsId;
import recommendationService.stats.AuthorDailyStatsRepository;
import recommendationService.stats.PodcastDailyStatsEntity;
import recommendationService.stats.PodcastDailyStatsId;
import recommendationService.stats.PodcastDailyStatsRepository;

@Service
public class PodcastActivityEventService {

    private static final Logger log = LoggerFactory.getLogger(PodcastActivityEventService.class);

    private static final BigDecimal PLAY_FINISHED_CATEGORY_WEIGHT = new BigDecimal("2.5");
    private static final BigDecimal PLAY_FINISHED_AUTHOR_WEIGHT = new BigDecimal("2.0");
    private static final BigDecimal LIKED_CATEGORY_WEIGHT = new BigDecimal("3.0");
    private static final BigDecimal LIKED_AUTHOR_WEIGHT = new BigDecimal("2.5");
    private static final BigDecimal DISLIKED_CATEGORY_WEIGHT = new BigDecimal("-2.0");
    private static final BigDecimal DISLIKED_AUTHOR_WEIGHT = new BigDecimal("-1.5");
    private static final BigDecimal AUTHOR_FOLLOWED_WEIGHT = new BigDecimal("5.0");
    private static final BigDecimal AUTHOR_UNFOLLOWED_WEIGHT = new BigDecimal("-4.0");

    private static final Set<RecommendationEventType> ACTIVITY_EVENT_TYPES = Set.of(
            RecommendationEventType.PODCAST_PLAY_FINISHED,
            RecommendationEventType.PODCAST_LIKED,
            RecommendationEventType.PODCAST_DISLIKED,
            RecommendationEventType.AUTHOR_FOLLOWED,
            RecommendationEventType.AUTHOR_UNFOLLOWED
    );

    private final RecommendationEventMapper eventMapper;
    private final ProcessedEventWriter processedEventWriter;
    private final PodcastCatalogSnapshotRepository podcastCatalogSnapshotRepository;
    private final UserPodcastInteractionRepository interactionRepository;
    private final UserCategoryInterestRepository categoryInterestRepository;
    private final UserAuthorInterestRepository authorInterestRepository;
    private final PodcastDailyStatsRepository podcastDailyStatsRepository;
    private final AuthorDailyStatsRepository authorDailyStatsRepository;
    private final MeterRegistry meterRegistry;

    public PodcastActivityEventService(
            RecommendationEventMapper eventMapper,
            ProcessedEventWriter processedEventWriter,
            PodcastCatalogSnapshotRepository podcastCatalogSnapshotRepository,
            UserPodcastInteractionRepository interactionRepository,
            UserCategoryInterestRepository categoryInterestRepository,
            UserAuthorInterestRepository authorInterestRepository,
            PodcastDailyStatsRepository podcastDailyStatsRepository,
            AuthorDailyStatsRepository authorDailyStatsRepository,
            MeterRegistry meterRegistry
    ) {
        this.eventMapper = eventMapper;
        this.processedEventWriter = processedEventWriter;
        this.podcastCatalogSnapshotRepository = podcastCatalogSnapshotRepository;
        this.interactionRepository = interactionRepository;
        this.categoryInterestRepository = categoryInterestRepository;
        this.authorInterestRepository = authorInterestRepository;
        this.podcastDailyStatsRepository = podcastDailyStatsRepository;
        this.authorDailyStatsRepository = authorDailyStatsRepository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public ActivityEventHandlingResult handle(String rawEventJson) {
        ParsedDomainEvent parsedEvent = eventMapper.read(rawEventJson);
        DomainEventEnvelope<?> envelope = parsedEvent.envelope();
        String eventId = envelope.eventId().toString();

        if (processedEventWriter.insertIfAbsent(
                eventId,
                envelope.eventType(),
                envelope.eventVersion(),
                Instant.now()
        ) == 0) {
            meterRegistry.counter("recommendation.events.duplicates").increment();
            log.info("recommendation_activity_event_duplicate eventId={} eventType={}", eventId, envelope.eventType());
            return ActivityEventHandlingResult.DUPLICATE;
        }

        if (!parsedEvent.knownEventType()) {
            log.info("recommendation_activity_event_ignored eventId={} eventType={}", eventId, envelope.eventType());
            return ActivityEventHandlingResult.IGNORED;
        }
        if (!ACTIVITY_EVENT_TYPES.contains(parsedEvent.eventType())) {
            throw new RecommendationEventDeserializationException(
                    "Recommendation event is routed to the wrong activity topic: " + envelope.eventType()
            );
        }

        applyActivityEvent(parsedEvent.eventType(), envelope);
        meterRegistry.counter("recommendation.events.processed").increment();
        log.info("recommendation_activity_event_processed eventId={} eventType={}", eventId, envelope.eventType());
        return ActivityEventHandlingResult.PROCESSED;
    }

    private void applyActivityEvent(RecommendationEventType eventType, DomainEventEnvelope<?> envelope) {
        switch (eventType) {
            case PODCAST_PLAY_FINISHED -> handlePlayFinished((PodcastPlayFinishedPayload) envelope.payload(), envelope.occurredAt());
            case PODCAST_LIKED -> handleLiked((PodcastLikedPayload) envelope.payload(), envelope.occurredAt());
            case PODCAST_DISLIKED -> handleDisliked((PodcastDislikedPayload) envelope.payload(), envelope.occurredAt());
            case AUTHOR_FOLLOWED -> handleAuthorFollowed((AuthorFollowedPayload) envelope.payload(), envelope.occurredAt());
            case AUTHOR_UNFOLLOWED -> handleAuthorUnfollowed((AuthorUnfollowedPayload) envelope.payload(), envelope.occurredAt());
            default -> throw new IllegalArgumentException("Unsupported activity event type: " + eventType.value());
        }
    }

    private void handlePlayFinished(PodcastPlayFinishedPayload payload, Instant eventTime) {
        PodcastMetadata metadata = enrich(payload.podcastId().toString(), payload.authorId(), payload.categoryId());
        UserPodcastInteractionEntity interaction = interaction(payload.userId().toString(), payload.podcastId().toString(), eventTime);
        interaction.incrementPlayFinishedCount();
        interaction.updateMaxProgressPercent(payload.progressPercent());
        interaction.touch(eventTime);
        interactionRepository.save(interaction);

        applyCategoryInterest(payload.userId().toString(), metadata.categoryId(), PLAY_FINISHED_CATEGORY_WEIGHT, eventTime);
        applyAuthorInterest(payload.userId().toString(), metadata.authorId(), PLAY_FINISHED_AUTHOR_WEIGHT, eventTime);
        PodcastDailyStatsEntity podcastStats = podcastStats(payload.podcastId().toString(), eventTime);
        podcastStats.incrementPlayFinished(eventTime);
        podcastDailyStatsRepository.save(podcastStats);
        if (metadata.authorId() == null) {
            warnMissing("authorId", RecommendationEventType.PODCAST_PLAY_FINISHED.value(), payload.podcastId().toString());
        } else {
            AuthorDailyStatsEntity authorStats = authorStats(metadata.authorId().toString(), eventTime);
            authorStats.incrementPlayFinished(eventTime);
            authorDailyStatsRepository.save(authorStats);
        }
    }

    private void handleLiked(PodcastLikedPayload payload, Instant eventTime) {
        PodcastMetadata metadata = enrich(payload.podcastId().toString(), payload.authorId(), payload.categoryId());
        UserPodcastInteractionEntity interaction = interaction(payload.userId().toString(), payload.podcastId().toString(), eventTime);
        interaction.markLiked();
        interaction.touch(eventTime);
        interactionRepository.save(interaction);

        applyCategoryInterest(payload.userId().toString(), metadata.categoryId(), LIKED_CATEGORY_WEIGHT, eventTime);
        applyAuthorInterest(payload.userId().toString(), metadata.authorId(), LIKED_AUTHOR_WEIGHT, eventTime);
        PodcastDailyStatsEntity podcastStats = podcastStats(payload.podcastId().toString(), eventTime);
        podcastStats.incrementLike(eventTime);
        podcastDailyStatsRepository.save(podcastStats);
        if (metadata.authorId() == null) {
            warnMissing("authorId", RecommendationEventType.PODCAST_LIKED.value(), payload.podcastId().toString());
        } else {
            AuthorDailyStatsEntity authorStats = authorStats(metadata.authorId().toString(), eventTime);
            authorStats.incrementLike(eventTime);
            authorDailyStatsRepository.save(authorStats);
        }
    }

    private void handleDisliked(PodcastDislikedPayload payload, Instant eventTime) {
        PodcastMetadata metadata = enrich(payload.podcastId().toString(), payload.authorId(), payload.categoryId());
        UserPodcastInteractionEntity interaction = interaction(payload.userId().toString(), payload.podcastId().toString(), eventTime);
        interaction.markDisliked();
        interaction.touch(eventTime);
        interactionRepository.save(interaction);

        applyCategoryInterest(payload.userId().toString(), metadata.categoryId(), DISLIKED_CATEGORY_WEIGHT, eventTime);
        applyAuthorInterest(payload.userId().toString(), metadata.authorId(), DISLIKED_AUTHOR_WEIGHT, eventTime);
        PodcastDailyStatsEntity podcastStats = podcastStats(payload.podcastId().toString(), eventTime);
        podcastStats.incrementDislike(eventTime);
        podcastDailyStatsRepository.save(podcastStats);
        if (metadata.authorId() == null) {
            warnMissing("authorId", RecommendationEventType.PODCAST_DISLIKED.value(), payload.podcastId().toString());
        } else {
            AuthorDailyStatsEntity authorStats = authorStats(metadata.authorId().toString(), eventTime);
            authorStats.incrementDislike(eventTime);
            authorDailyStatsRepository.save(authorStats);
        }
    }

    private void handleAuthorFollowed(AuthorFollowedPayload payload, Instant eventTime) {
        applyAuthorInterest(payload.userId().toString(), payload.authorId(), AUTHOR_FOLLOWED_WEIGHT, eventTime);
        AuthorDailyStatsEntity authorStats = authorStats(payload.authorId().toString(), eventTime);
        authorStats.incrementFollowed(eventTime);
        authorDailyStatsRepository.save(authorStats);
    }

    private void handleAuthorUnfollowed(AuthorUnfollowedPayload payload, Instant eventTime) {
        applyAuthorInterest(payload.userId().toString(), payload.authorId(), AUTHOR_UNFOLLOWED_WEIGHT, eventTime);
        AuthorDailyStatsEntity authorStats = authorStats(payload.authorId().toString(), eventTime);
        authorStats.incrementUnfollowed(eventTime);
        authorDailyStatsRepository.save(authorStats);
    }

    private UserPodcastInteractionEntity interaction(String userId, String podcastId, Instant eventTime) {
        String interactionId = userId + ":" + podcastId;
        return interactionRepository.findById(interactionId)
                .orElseGet(() -> new UserPodcastInteractionEntity(userId, podcastId, eventTime));
    }

    private void applyCategoryInterest(String userId, java.util.UUID categoryId, BigDecimal delta, Instant eventTime) {
        if (categoryId == null) {
            warnMissing("categoryId", "podcast.activity", null);
            return;
        }

        UserCategoryInterestId id = new UserCategoryInterestId(userId, categoryId.toString());
        UserCategoryInterestEntity interest = categoryInterestRepository.findById(id)
                .orElseGet(() -> new UserCategoryInterestEntity(userId, categoryId.toString(), eventTime));
        interest.apply(delta, eventTime);
        categoryInterestRepository.save(interest);
    }

    private void applyAuthorInterest(String userId, java.util.UUID authorId, BigDecimal delta, Instant eventTime) {
        if (authorId == null) {
            warnMissing("authorId", "podcast.activity", null);
            return;
        }

        UserAuthorInterestId id = new UserAuthorInterestId(userId, authorId.toString());
        UserAuthorInterestEntity interest = authorInterestRepository.findById(id)
                .orElseGet(() -> new UserAuthorInterestEntity(userId, authorId.toString(), eventTime));
        interest.apply(delta, eventTime);
        authorInterestRepository.save(interest);
    }

    private PodcastDailyStatsEntity podcastStats(String podcastId, Instant eventTime) {
        PodcastDailyStatsId id = new PodcastDailyStatsId(podcastId, eventDate(eventTime));
        PodcastDailyStatsEntity stats = podcastDailyStatsRepository.findById(id)
                .orElseGet(() -> new PodcastDailyStatsEntity(podcastId, id.getStatDate(), eventTime));
        return stats;
    }

    private AuthorDailyStatsEntity authorStats(String authorId, Instant eventTime) {
        AuthorDailyStatsId id = new AuthorDailyStatsId(authorId, eventDate(eventTime));
        AuthorDailyStatsEntity stats = authorDailyStatsRepository.findById(id)
                .orElseGet(() -> new AuthorDailyStatsEntity(authorId, id.getStatDate(), eventTime));
        return stats;
    }

    private LocalDate eventDate(Instant eventTime) {
        return eventTime.atZone(ZoneOffset.UTC).toLocalDate();
    }

    private PodcastMetadata enrich(String podcastId, java.util.UUID authorId, java.util.UUID categoryId) {
        if (authorId != null && categoryId != null) {
            return new PodcastMetadata(authorId, categoryId);
        }
        PodcastCatalogSnapshotEntity snapshot = podcastCatalogSnapshotRepository.findById(podcastId).orElse(null);
        return new PodcastMetadata(
                authorId != null ? authorId : uuidOrNull(snapshot == null ? null : snapshot.getAuthorId()),
                categoryId != null ? categoryId : uuidOrNull(snapshot == null ? null : snapshot.getCategoryId())
        );
    }

    private java.util.UUID uuidOrNull(String value) {
        return value == null || value.isBlank() ? null : java.util.UUID.fromString(value);
    }

    private void warnMissing(String field, String eventType, String aggregateId) {
        log.warn("recommendation_activity_event_missing_optional field={} eventType={} aggregateId={}", field, eventType, aggregateId);
    }

    private record PodcastMetadata(java.util.UUID authorId, java.util.UUID categoryId) {
    }
}
