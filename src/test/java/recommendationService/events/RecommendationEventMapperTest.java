package recommendationService.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.stream.Stream;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import recommendationService.events.payload.AuthorFollowedPayload;
import recommendationService.events.payload.AuthorUnfollowedPayload;
import recommendationService.events.payload.PlaylistCreatedPayload;
import recommendationService.events.payload.PlaylistDeletedPayload;
import recommendationService.events.payload.PlaylistUpdatedPayload;
import recommendationService.events.payload.PodcastDeletedPayload;
import recommendationService.events.payload.PodcastDislikedPayload;
import recommendationService.events.payload.PodcastLikedPayload;
import recommendationService.events.payload.PodcastPlayFinishedPayload;
import recommendationService.events.payload.PodcastPublishedPayload;
import recommendationService.events.payload.PodcastUpdatedPayload;

class RecommendationEventMapperTest {

    private static final String EVENT_ID = "01000000-0000-0000-0000-000000000001";
    private static final String PODCAST_ID = "10000000-0000-0000-0000-000000000001";
    private static final String AUTHOR_ID = "20000000-0000-0000-0000-000000000001";
    private static final String CATEGORY_ID = "30000000-0000-0000-0000-000000000001";
    private static final String PLAYLIST_ID = "40000000-0000-0000-0000-000000000001";
    private static final String USER_ID = "50000000-0000-0000-0000-000000000001";
    private static final String OCCURRED_AT = "2026-06-01T10:15:30Z";

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final RecommendationEventMapper mapper = new RecommendationEventMapper(new ObjectMapper(), validator);

    @Test
    void readsPodcastCoreContractSnapshots() throws IOException {
        for (String resource : new String[]{
                "podcast.published.v1.json",
                "podcast.play_finished.v1.json",
                "podcast.liked.v1.json",
                "playlist.updated.v1.json"
        }) {
            try (var stream = getClass().getResourceAsStream("/recommendation-events/" + resource)) {
                assertThat(stream).as(resource).isNotNull();
                assertThat(mapper.read(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).knownEventType())
                        .as(resource)
                        .isTrue();
            }
        }
    }

    @ParameterizedTest
    @MethodSource("jsonContracts")
    void readsPodcastCoreJsonContracts(
            String eventType,
            String payloadJson,
            Class<?> expectedPayloadType
    ) {
        ParsedDomainEvent event = mapper.read(envelope(eventType, payloadJson));

        assertThat(event.knownEventType()).isTrue();
        assertThat(event.eventType().value()).isEqualTo(eventType);
        assertThat(event.envelope().eventId().toString()).isEqualTo(EVENT_ID);
        assertThat(event.envelope().eventVersion()).isEqualTo(1);
        assertThat(event.envelope().producer()).isEqualTo("podcast-core");
        assertThat(event.envelope().userId().toString()).isEqualTo(USER_ID);
        assertThat(event.envelope().payload()).isInstanceOf(expectedPayloadType);
    }

    @ParameterizedTest
    @MethodSource("jsonContracts")
    void ignoresUnknownFieldsInEnvelopeAndPayload(
            String eventType,
            String payloadJson,
            Class<?> expectedPayloadType
    ) {
        ParsedDomainEvent event = mapper.read(envelopeWithExtraFields(eventType, payloadJson));

        assertThat(event.knownEventType()).isTrue();
        assertThat(event.envelope().payload()).isInstanceOf(expectedPayloadType);
    }

    @ParameterizedTest
    @MethodSource("jsonContracts")
    void keepsUnknownEventTypeAsRawEnvelope(
            String ignoredEventType,
            String payloadJson,
            Class<?> ignoredPayloadType
    ) {
        ParsedDomainEvent event = mapper.read(envelope("podcast.future_event.v1", payloadJson));

        assertThat(event.knownEventType()).isFalse();
        assertThat(event.eventType()).isNull();
        assertThat(event.envelope().payload()).isInstanceOf(JsonNode.class);
    }

    @ParameterizedTest
    @MethodSource("invalidContracts")
    void rejectsInvalidContracts(String json, String expectedMessage) {
        assertThatThrownBy(() -> mapper.read(json))
                .isInstanceOf(RecommendationEventDeserializationException.class)
                .hasMessageContaining(expectedMessage);
    }

    private static Stream<Arguments> jsonContracts() {
        return Stream.of(
                Arguments.of(
                        "podcast.published.v1",
                        """
                                {
                                  "podcastId": "%s",
                                  "authorId": "%s",
                                  "categoryId": "%s",
                                  "title": "Podcast title",
                                  "publishedAt": "%s"
                                }
                                """.formatted(PODCAST_ID, AUTHOR_ID, CATEGORY_ID, OCCURRED_AT),
                        PodcastPublishedPayload.class
                ),
                Arguments.of(
                        "podcast.updated.v1",
                        """
                                {
                                  "podcastId": "%s",
                                  "authorId": "%s",
                                  "categoryId": "%s",
                                  "title": "Updated podcast title",
                                  "updatedAt": "%s"
                                }
                                """.formatted(PODCAST_ID, AUTHOR_ID, CATEGORY_ID, OCCURRED_AT),
                        PodcastUpdatedPayload.class
                ),
                Arguments.of(
                        "podcast.deleted.v1",
                        """
                                {
                                  "podcastId": "%s",
                                  "authorId": "%s",
                                  "deletedAt": "%s"
                                }
                                """.formatted(PODCAST_ID, AUTHOR_ID, OCCURRED_AT),
                        PodcastDeletedPayload.class
                ),
                Arguments.of(
                        "podcast.play_finished.v1",
                        """
                                {
                                  "podcastId": "%s",
                                  "userId": "%s",
                                  "progressSeconds": 1800,
                                  "finishedAt": "%s"
                                }
                                """.formatted(PODCAST_ID, USER_ID, OCCURRED_AT),
                        PodcastPlayFinishedPayload.class
                ),
                Arguments.of(
                        "podcast.liked.v1",
                        """
                                {
                                  "podcastId": "%s",
                                  "userId": "%s",
                                  "likedAt": "%s"
                                }
                                """.formatted(PODCAST_ID, USER_ID, OCCURRED_AT),
                        PodcastLikedPayload.class
                ),
                Arguments.of(
                        "podcast.disliked.v1",
                        """
                                {
                                  "podcastId": "%s",
                                  "userId": "%s",
                                  "dislikedAt": "%s"
                                }
                                """.formatted(PODCAST_ID, USER_ID, OCCURRED_AT),
                        PodcastDislikedPayload.class
                ),
                Arguments.of(
                        "author.followed.v1",
                        """
                                {
                                  "authorId": "%s",
                                  "userId": "%s",
                                  "followedAt": "%s"
                                }
                                """.formatted(AUTHOR_ID, USER_ID, OCCURRED_AT),
                        AuthorFollowedPayload.class
                ),
                Arguments.of(
                        "author.unfollowed.v1",
                        """
                                {
                                  "authorId": "%s",
                                  "userId": "%s",
                                  "unfollowedAt": "%s"
                                }
                                """.formatted(AUTHOR_ID, USER_ID, OCCURRED_AT),
                        AuthorUnfollowedPayload.class
                ),
                Arguments.of(
                        "playlist.created.v1",
                        """
                                {
                                  "playlistId": "%s",
                                  "ownerUserId": "%s",
                                  "title": "Playlist title",
                                  "publicPlaylist": true,
                                  "createdAt": "%s"
                                }
                                """.formatted(PLAYLIST_ID, USER_ID, OCCURRED_AT),
                        PlaylistCreatedPayload.class
                ),
                Arguments.of(
                        "playlist.updated.v1",
                        """
                                {
                                  "playlistId": "%s",
                                  "ownerUserId": "%s",
                                  "title": "Updated playlist title",
                                  "publicPlaylist": false,
                                  "updatedAt": "%s"
                                }
                                """.formatted(PLAYLIST_ID, USER_ID, OCCURRED_AT),
                        PlaylistUpdatedPayload.class
                ),
                Arguments.of(
                        "playlist.deleted.v1",
                        """
                                {
                                  "playlistId": "%s",
                                  "ownerUserId": "%s",
                                  "deletedAt": "%s"
                                }
                                """.formatted(PLAYLIST_ID, USER_ID, OCCURRED_AT),
                        PlaylistDeletedPayload.class
                )
        );
    }

    private static Stream<Arguments> invalidContracts() {
        return Stream.of(
                Arguments.of(
                        """
                                {
                                  "eventType": "podcast.published.v1",
                                  "eventVersion": 1,
                                  "producer": "podcast-core",
                                  "occurredAt": "%s",
                                  "payload": {}
                                }
                                """.formatted(OCCURRED_AT),
                        "missing eventId"
                ),
                Arguments.of(
                        envelope("podcast.published.v1", """
                                {
                                  "podcastId": "not-a-uuid",
                                  "authorId": "%s",
                                  "categoryId": "%s",
                                  "title": "Podcast title",
                                  "publishedAt": "%s"
                                }
                                """.formatted(AUTHOR_ID, CATEGORY_ID, OCCURRED_AT)),
                        "Failed to deserialize recommendation event payload"
                ),
                Arguments.of(
                        envelope("podcast.published.v1", """
                                {
                                  "authorId": "%s",
                                  "categoryId": "%s",
                                  "title": "Podcast title",
                                  "publishedAt": "%s"
                                }
                                """.formatted(AUTHOR_ID, CATEGORY_ID, OCCURRED_AT)),
                        "payload is invalid"
                ),
                Arguments.of(
                        envelope("podcast.published.v1", """
                                {
                                  "podcastId": "%s",
                                  "authorId": "%s",
                                  "categoryId": "%s",
                                  "title": "Podcast title",
                                  "publishedAt": "%s"
                                }
                                """.formatted(PODCAST_ID, AUTHOR_ID, CATEGORY_ID, OCCURRED_AT))
                                .replace("\"eventVersion\": 1", "\"eventVersion\": 2"),
                        "Unsupported recommendation event version"
                )
        );
    }

    private static String envelope(String eventType, String payloadJson) {
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
                """.formatted(EVENT_ID, eventType, OCCURRED_AT, USER_ID, payloadJson);
    }

    private static String envelopeWithExtraFields(String eventType, String payloadJson) {
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
                  "ignoredEnvelopeField": "ignored",
                  "payload": %s
                }
                """.formatted(EVENT_ID, eventType, OCCURRED_AT, USER_ID, addIgnoredPayloadField(payloadJson));
    }

    private static String addIgnoredPayloadField(String payloadJson) {
        return payloadJson.replaceFirst("\\{", "{\"ignoredPayloadField\":\"ignored\",");
    }
}
