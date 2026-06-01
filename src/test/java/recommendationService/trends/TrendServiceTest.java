package recommendationService.trends;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TrendServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-02T10:15:30Z"), ZoneOffset.UTC);

    private final TrendQueryRepository repository = Mockito.mock(TrendQueryRepository.class);
    private final TrendService service = new TrendService(repository, CLOCK);

    @Test
    void ranksRowsAndUsesDefaultLimit() {
        when(repository.findPodcastTrends(LocalDate.parse("2026-06-02"), LocalDate.parse("2026-06-02"), null, 50))
                .thenReturn(List.of(
                        new TrendRow("podcast-1", new BigDecimal("10"), "PODCAST_ACTIVITY", "reason", Map.of()),
                        new TrendRow("podcast-2", new BigDecimal("8"), "PODCAST_ACTIVITY", "reason", Map.of())
                ));

        List<TrendItemResponse> response = service.podcastTrends(TrendPeriod.DAY, null, null);

        assertThat(response).extracting(TrendItemResponse::rank).containsExactly(1, 2);
        verify(repository).findPodcastTrends(LocalDate.parse("2026-06-02"), LocalDate.parse("2026-06-02"), null, 50);
    }

    @Test
    void weekPeriodUsesSevenCalendarDatesIncludingToday() {
        service.authorTrends(TrendPeriod.WEEK, 25);

        verify(repository).findAuthorTrends(LocalDate.parse("2026-05-27"), LocalDate.parse("2026-06-02"), 25);
    }

    @Test
    void rejectsLimitAboveMax() {
        assertThatThrownBy(() -> service.playlistTrends(TrendPeriod.MONTH, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit must be between 1 and 100");
    }
}
