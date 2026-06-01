package recommendationService.trends;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TrendControllerTest {

    private final TrendService trendService = Mockito.mock(TrendService.class);
    private final TrendController controller = new TrendController(trendService);

    @Test
    void podcastEndpointDelegatesWithParsedPeriod() {
        List<TrendItemResponse> expected = List.of(new TrendItemResponse(
                "podcast-1",
                1,
                BigDecimal.TEN,
                "PODCAST_ACTIVITY",
                "reason",
                Map.of("title", "Podcast")
        ));
        when(trendService.podcastTrends(TrendPeriod.WEEK, "category-1", 10)).thenReturn(expected);

        List<TrendItemResponse> response = controller.podcastTrends("week", "category-1", 10).getBody();

        assertThat(response).isEqualTo(expected);
        verify(trendService).podcastTrends(TrendPeriod.WEEK, "category-1", 10);
    }

    @Test
    void authorEndpointDelegatesWithDefaultPeriodValue() {
        controller.authorTrends("day", 50);

        verify(trendService).authorTrends(TrendPeriod.DAY, 50);
    }

    @Test
    void rejectsUnknownPeriod() {
        assertThatThrownBy(() -> controller.playlistTrends("year", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("period must be one of: day, week, month");
    }
}
