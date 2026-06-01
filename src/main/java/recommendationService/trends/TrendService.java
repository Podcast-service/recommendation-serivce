package recommendationService.trends;

import java.time.Clock;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;

@Service
public class TrendService {

    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 100;

    private final TrendQueryRepository repository;
    private final Clock clock;

    public TrendService(TrendQueryRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<TrendItemResponse> podcastTrends(TrendPeriod period, String categoryId, Integer limit) {
        int normalizedLimit = normalizeLimit(limit);
        return rank(repository.findPodcastTrends(period.startDate(clock), period.endDate(clock), categoryId, normalizedLimit));
    }

    public List<TrendItemResponse> authorTrends(TrendPeriod period, Integer limit) {
        int normalizedLimit = normalizeLimit(limit);
        return rank(repository.findAuthorTrends(period.startDate(clock), period.endDate(clock), normalizedLimit));
    }

    public List<TrendItemResponse> playlistTrends(TrendPeriod period, Integer limit) {
        int normalizedLimit = normalizeLimit(limit);
        return rank(repository.findPlaylistTrends(period.startDate(clock), period.endDate(clock), normalizedLimit));
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }
        return limit;
    }

    private List<TrendItemResponse> rank(List<TrendRow> rows) {
        return IntStream.range(0, rows.size())
                .mapToObj(index -> {
                    TrendRow row = rows.get(index);
                    return new TrendItemResponse(
                        row.itemId(),
                        index + 1,
                        row.score(),
                        row.reasonCode(),
                        row.reasonText(),
                        row.metadata()
                    );
                })
                .toList();
    }
}
