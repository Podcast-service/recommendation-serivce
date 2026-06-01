package recommendationService.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import recommendationService.events.ProcessedEventRepository;
import recommendationService.stats.AuthorDailyStatsId;
import recommendationService.stats.AuthorDailyStatsRepository;
import recommendationService.stats.PodcastDailyStatsId;
import recommendationService.stats.PodcastDailyStatsRepository;

@SpringBootTest
@ActiveProfiles("test")
class PodcastActivityEventServiceTest {

    private static final String EVENT_ID = "02000000-0000-0000-0000-000000000001";
    private static final String EVENT_ID_2 = "02000000-0000-0000-0000-000000000002";
    private static final String PODCAST_ID = "10000000-0000-0000-0000-000000000001";
    private static final String AUTHOR_ID = "20000000-0000-0000-0000-000000000001";
    private static final String CATEGORY_ID = "30000000-0000-0000-0000-000000000001";
    private static final String USER_ID = "50000000-0000-0000-0000-000000000001";
    private static final String OCCURRED_AT = "2026-06-01T10:15:30Z";
    private static final LocalDate STAT_DATE = LocalDate.parse("2026-06-01");

    @Autowired
    private PodcastActivityEventService service;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private UserPodcastInteractionRepository interactionRepository;

    @Autowired
    private UserCategoryInterestRepository categoryInterestRepository;

    @Autowired
    private UserAuthorInterestRepository authorInterestRepository;

    @Autowired
    private PodcastDailyStatsRepository podcastDailyStatsRepository;

    @Autowired
    private AuthorDailyStatsRepository authorDailyStatsRepository;

    @BeforeEach
    void cleanDatabase() {
        processedEventRepository.deleteAll();
        interactionRepository.deleteAll();
        categoryInterestRepository.deleteAll();
        authorInterestRepository.deleteAll();
        podcastDailyStatsRepository.deleteAll();
        authorDailyStatsRepository.deleteAll();
    }

    @Test
    void likedIncreasesInterestsAndStats() {
        ActivityEventHandlingResult result = service.handle(podcastLiked(EVENT_ID, true));

        assertThat(result).isEqualTo(ActivityEventHandlingResult.PROCESSED);
        assertThat(categoryInterest().getInterestScore()).isEqualByComparingTo(new BigDecimal("3.0"));
        assertThat(authorInterest().getInterestScore()).isEqualByComparingTo(new BigDecimal("2.5"));
        assertThat(podcastStats().getLikeCount()).isEqualTo(1);
        assertThat(authorStats().getLikeCount()).isEqualTo(1);
        assertThat(interaction().isLiked()).isTrue();
    }

    @Test
    void dislikedDecreasesInterests() {
        ActivityEventHandlingResult result = service.handle(podcastDisliked(EVENT_ID, true));

        assertThat(result).isEqualTo(ActivityEventHandlingResult.PROCESSED);
        assertThat(categoryInterest().getInterestScore()).isEqualByComparingTo(new BigDecimal("-2.0"));
        assertThat(authorInterest().getInterestScore()).isEqualByComparingTo(new BigDecimal("-1.5"));
        assertThat(podcastStats().getDislikeCount()).isEqualTo(1);
        assertThat(authorStats().getDislikeCount()).isEqualTo(1);
        assertThat(interaction().isDisliked()).isTrue();
    }

    @Test
    void playFinishedIncreasesCountAndMaxProgressPercent() {
        service.handle(podcastPlayFinished(EVENT_ID, "45.50", true));

        ActivityEventHandlingResult result = service.handle(podcastPlayFinished(EVENT_ID_2, "80.00", true));

        assertThat(result).isEqualTo(ActivityEventHandlingResult.PROCESSED);
        assertThat(interaction().getPlayFinishedCount()).isEqualTo(2);
        assertThat(interaction().getMaxProgressPercent()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(categoryInterest().getInterestScore()).isEqualByComparingTo(new BigDecimal("5.0"));
        assertThat(authorInterest().getInterestScore()).isEqualByComparingTo(new BigDecimal("4.0"));
        assertThat(podcastStats().getPlayFinishedCount()).isEqualTo(2);
        assertThat(authorStats().getPlayFinishedCount()).isEqualTo(2);
    }

    @Test
    void authorFollowedIncreasesAuthorInterest() {
        ActivityEventHandlingResult result = service.handle(authorFollowed(EVENT_ID));

        assertThat(result).isEqualTo(ActivityEventHandlingResult.PROCESSED);
        assertThat(authorInterest().getInterestScore()).isEqualByComparingTo(new BigDecimal("5.0"));
        assertThat(authorInterest().getSignalCount()).isEqualTo(1);
        assertThat(authorStats().getFollowedCount()).isEqualTo(1);
    }

    @Test
    void duplicateEventDoesNotChangeDataSecondTime() {
        String event = podcastLiked(EVENT_ID, true);
        service.handle(event);

        ActivityEventHandlingResult result = service.handle(event);

        assertThat(result).isEqualTo(ActivityEventHandlingResult.DUPLICATE);
        assertThat(categoryInterest().getInterestScore()).isEqualByComparingTo(new BigDecimal("3.0"));
        assertThat(authorInterest().getInterestScore()).isEqualByComparingTo(new BigDecimal("2.5"));
        assertThat(podcastStats().getLikeCount()).isEqualTo(1);
        assertThat(authorStats().getLikeCount()).isEqualTo(1);
    }

    @Test
    void missingOptionalFieldsDoNotBreakProcessing() {
        ActivityEventHandlingResult result = service.handle(podcastLiked(EVENT_ID, false));

        assertThat(result).isEqualTo(ActivityEventHandlingResult.PROCESSED);
        assertThat(interaction().isLiked()).isTrue();
        assertThat(podcastStats().getLikeCount()).isEqualTo(1);
        assertThat(categoryInterestRepository.findAll()).isEmpty();
        assertThat(authorInterestRepository.findAll()).isEmpty();
        assertThat(authorDailyStatsRepository.findAll()).isEmpty();
    }

    private UserPodcastInteractionEntity interaction() {
        return interactionRepository.findById(USER_ID + ":" + PODCAST_ID).orElseThrow();
    }

    private UserCategoryInterestEntity categoryInterest() {
        return categoryInterestRepository.findById(new UserCategoryInterestId(USER_ID, CATEGORY_ID)).orElseThrow();
    }

    private UserAuthorInterestEntity authorInterest() {
        return authorInterestRepository.findById(new UserAuthorInterestId(USER_ID, AUTHOR_ID)).orElseThrow();
    }

    private recommendationService.stats.PodcastDailyStatsEntity podcastStats() {
        return podcastDailyStatsRepository.findById(new PodcastDailyStatsId(PODCAST_ID, STAT_DATE)).orElseThrow();
    }

    private recommendationService.stats.AuthorDailyStatsEntity authorStats() {
        return authorDailyStatsRepository.findById(new AuthorDailyStatsId(AUTHOR_ID, STAT_DATE)).orElseThrow();
    }

    private String podcastLiked(String eventId, boolean withOptionalFields) {
        return envelope(eventId, "podcast.liked.v1", """
                {
                  "podcastId": "%s",
                  "userId": "%s",
                  "likedAt": "%s"%s
                }
                """.formatted(PODCAST_ID, USER_ID, OCCURRED_AT, optionalPodcastFields(withOptionalFields)));
    }

    private String podcastDisliked(String eventId, boolean withOptionalFields) {
        return envelope(eventId, "podcast.disliked.v1", """
                {
                  "podcastId": "%s",
                  "userId": "%s",
                  "dislikedAt": "%s"%s
                }
                """.formatted(PODCAST_ID, USER_ID, OCCURRED_AT, optionalPodcastFields(withOptionalFields)));
    }

    private String podcastPlayFinished(String eventId, String progressPercent, boolean withOptionalFields) {
        String optionalFields = withOptionalFields
                ? optionalPodcastFields(true) + """
                  ,
                  "progressPercent": %s
                """.formatted(progressPercent)
                : "";
        return envelope(eventId, "podcast.play_finished.v1", """
                {
                  "podcastId": "%s",
                  "userId": "%s",
                  "progressSeconds": 1800,
                  "finishedAt": "%s"%s
                }
                """.formatted(PODCAST_ID, USER_ID, OCCURRED_AT, optionalFields));
    }

    private String authorFollowed(String eventId) {
        return envelope(eventId, "author.followed.v1", """
                {
                  "authorId": "%s",
                  "userId": "%s",
                  "followedAt": "%s"
                }
                """.formatted(AUTHOR_ID, USER_ID, OCCURRED_AT));
    }

    private String optionalPodcastFields(boolean include) {
        if (!include) {
            return "";
        }
        return """
          ,
          "authorId": "%s",
          "categoryId": "%s"
        """.formatted(AUTHOR_ID, CATEGORY_ID);
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
