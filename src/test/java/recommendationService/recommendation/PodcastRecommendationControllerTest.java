package recommendationService.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PodcastRecommendationControllerTest {

    private final PodcastRecommendationService service = Mockito.mock(PodcastRecommendationService.class);
    private final PodcastRecommendationController controller = new PodcastRecommendationController(service);

    @Test
    void endpointDelegatesToService() {
        List<PodcastRecommendationResponse> expected = List.of(new PodcastRecommendationResponse(
                "podcast-1",
                1,
                new BigDecimal("82.5000"),
                "TOP_CATEGORY",
                "reason",
                Map.of("title", "Podcast")
        ));
        when(service.recommendPodcasts("user-1", 20, "category-1", true)).thenReturn(expected);

        List<PodcastRecommendationResponse> response = controller
                .recommendPodcasts("user-1", 20, "category-1", true)
                .getBody();

        assertThat(response).isEqualTo(expected);
        verify(service).recommendPodcasts("user-1", 20, "category-1", true);
    }
}
