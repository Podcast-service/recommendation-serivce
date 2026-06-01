package recommendationService.recommendation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;

@Service
public class RecommendationBlocksService {

    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 100;
    private static final int CANDIDATE_MULTIPLIER = 10;

    private final RecommendationBlocksQueryRepository repository;
    private final PodcastRecommendationService podcastRecommendationService;
    private final Clock clock;

    public RecommendationBlocksService(
            RecommendationBlocksQueryRepository repository,
            PodcastRecommendationService podcastRecommendationService,
            Clock clock
    ) {
        this.repository = repository;
        this.podcastRecommendationService = podcastRecommendationService;
        this.clock = clock;
    }

    public List<RecommendationItemResponse> feed(String userId, Integer limit) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        int normalizedLimit = normalizeLimit(limit);
        List<RecommendationItemResponse> podcasts = podcastRecommendationService
                .recommendPodcasts(userId, normalizedLimit, null, true)
                .stream()
                .map(item -> new RecommendationItemResponse(
                        "PODCAST",
                        item.itemId(),
                        item.rank(),
                        item.score(),
                        item.reasonCode(),
                        item.reasonText(),
                        item.metadata()
                ))
                .toList();
        List<RecommendationItemResponse> playlists = playlists(userId, normalizedLimit);
        return rank(
                java.util.stream.Stream.concat(podcasts.stream(), playlists.stream())
                        .sorted(Comparator.comparing(RecommendationItemResponse::score).reversed()
                                .thenComparing(RecommendationItemResponse::itemType)
                                .thenComparing(RecommendationItemResponse::itemId))
                        .limit(normalizedLimit)
                        .toList()
        );
    }

    public List<RecommendationItemResponse> playlists(String userId, Integer limit) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        int normalizedLimit = normalizeLimit(limit);
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        List<PlaylistRecommendationCandidate> candidates = repository.findPlaylistCandidates(
                userId,
                today.minusDays(6),
                today,
                normalizedLimit * CANDIDATE_MULTIPLIER
        );
        BigDecimal maxCategory = max(candidates.stream().map(PlaylistRecommendationCandidate::categoryInterestScore).toList());
        BigDecimal maxAuthor = max(candidates.stream().map(PlaylistRecommendationCandidate::authorInterestScore).toList());
        BigDecimal maxPopularity = max(candidates.stream().map(PlaylistRecommendationCandidate::popularityScore).toList());
        List<RecommendationItemResponse> scored = candidates.stream()
                .map(candidate -> scorePlaylist(candidate, maxCategory, maxAuthor, maxPopularity))
                .filter(item -> item.score().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(RecommendationItemResponse::score).reversed()
                        .thenComparing(RecommendationItemResponse::itemId))
                .limit(normalizedLimit)
                .toList();
        return rank(scored);
    }

    public List<RecommendationItemResponse> similarPodcasts(String podcastId, Integer limit) {
        if (podcastId == null || podcastId.isBlank()) {
            throw new IllegalArgumentException("podcastId is required");
        }
        int normalizedLimit = normalizeLimit(limit);
        SimilarPodcastSource source = repository.findSimilarPodcastSource(podcastId).orElse(null);
        if (source == null) {
            return List.of();
        }
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        List<SimilarPodcastCandidate> candidates = repository.findSimilarPodcastCandidates(
                podcastId,
                today.minusDays(6),
                today,
                normalizedLimit * CANDIDATE_MULTIPLIER
        );
        BigDecimal maxPopularity = max(candidates.stream().map(SimilarPodcastCandidate::popularityScore).toList());
        List<RecommendationItemResponse> scored = candidates.stream()
                .map(candidate -> scoreSimilarPodcast(source, candidate, maxPopularity))
                .filter(item -> item.score().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(RecommendationItemResponse::score).reversed()
                        .thenComparing(RecommendationItemResponse::itemId))
                .limit(normalizedLimit)
                .toList();
        return rank(scored);
    }

    public List<RecommendationItemResponse> similarAuthors(String authorId, Integer limit) {
        if (authorId == null || authorId.isBlank()) {
            throw new IllegalArgumentException("authorId is required");
        }
        int normalizedLimit = normalizeLimit(limit);
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        List<SimilarAuthorCandidate> candidates = repository.findSimilarAuthorCandidates(
                authorId,
                today.minusDays(6),
                today,
                normalizedLimit * CANDIDATE_MULTIPLIER
        );
        BigDecimal maxCategory = max(candidates.stream().map(SimilarAuthorCandidate::categoryScore).toList());
        BigDecimal maxAudience = max(candidates.stream().map(SimilarAuthorCandidate::audienceScore).toList());
        BigDecimal maxTrend = max(candidates.stream().map(SimilarAuthorCandidate::trendScore).toList());
        List<RecommendationItemResponse> scored = candidates.stream()
                .map(candidate -> scoreSimilarAuthor(candidate, maxCategory, maxAudience, maxTrend))
                .filter(item -> item.score().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(RecommendationItemResponse::score).reversed()
                        .thenComparing(RecommendationItemResponse::itemId))
                .limit(normalizedLimit)
                .toList();
        return rank(scored);
    }

    private RecommendationItemResponse scorePlaylist(
            PlaylistRecommendationCandidate candidate,
            BigDecimal maxCategory,
            BigDecimal maxAuthor,
            BigDecimal maxPopularity
    ) {
        BigDecimal category = normalize(candidate.categoryInterestScore(), maxCategory);
        BigDecimal author = normalize(candidate.authorInterestScore(), maxAuthor);
        BigDecimal popularity = normalize(candidate.popularityScore(), maxPopularity);
        BigDecimal quality = clamp01(nullSafe(candidate.qualityScore()));
        BigDecimal freshness = freshnessScore(candidate.updatedAt());
        BigDecimal score = category.multiply(new BigDecimal("0.35"))
                .add(author.multiply(new BigDecimal("0.25")))
                .add(popularity.multiply(new BigDecimal("0.20")))
                .add(quality.multiply(new BigDecimal("0.10")))
                .add(freshness.multiply(new BigDecimal("0.10")));
        return new RecommendationItemResponse(
                "PLAYLIST",
                candidate.playlistId(),
                0,
                toPercent(score),
                playlistReasonCode(category, author, popularity, quality, freshness),
                playlistReasonText(category, author, popularity, quality, freshness),
                Map.of(
                        "title", value(candidate.title()),
                        "ownerUserId", value(candidate.ownerUserId()),
                        "updatedAt", value(candidate.updatedAt())
                )
        );
    }

    private RecommendationItemResponse scoreSimilarPodcast(
            SimilarPodcastSource source,
            SimilarPodcastCandidate candidate,
            BigDecimal maxPopularity
    ) {
        BigDecimal sameCategory = equalsNullable(source.categoryId(), candidate.categoryId()) ? BigDecimal.ONE : BigDecimal.ZERO;
        BigDecimal sameAuthor = equalsNullable(source.authorId(), candidate.authorId()) ? BigDecimal.ONE : BigDecimal.ZERO;
        BigDecimal tagsOverlap = tagsOverlap(source.tags(), candidate.tags());
        BigDecimal similarDuration = durationSimilarity(source.durationSeconds(), candidate.durationSeconds());
        BigDecimal popularity = normalize(candidate.popularityScore(), maxPopularity);
        BigDecimal score = sameCategory.multiply(new BigDecimal("0.40"))
                .add(sameAuthor.multiply(new BigDecimal("0.25")))
                .add(tagsOverlap.multiply(new BigDecimal("0.20")))
                .add(similarDuration.multiply(new BigDecimal("0.10")))
                .add(popularity.multiply(new BigDecimal("0.05")));
        return new RecommendationItemResponse(
                "PODCAST",
                candidate.podcastId(),
                0,
                toPercent(score),
                similarPodcastReasonCode(sameCategory, sameAuthor, tagsOverlap, similarDuration, popularity),
                similarPodcastReasonText(sameCategory, sameAuthor, tagsOverlap, similarDuration, popularity),
                Map.of(
                        "title", value(candidate.title()),
                        "authorId", value(candidate.authorId()),
                        "categoryId", value(candidate.categoryId()),
                        "publishedAt", value(candidate.publishedAt())
                )
        );
    }

    private RecommendationItemResponse scoreSimilarAuthor(
            SimilarAuthorCandidate candidate,
            BigDecimal maxCategory,
            BigDecimal maxAudience,
            BigDecimal maxTrend
    ) {
        BigDecimal category = normalize(candidate.categoryScore(), maxCategory);
        BigDecimal audience = normalize(candidate.audienceScore(), maxAudience);
        BigDecimal trend = normalize(candidate.trendScore(), maxTrend);
        BigDecimal score = category.multiply(new BigDecimal("0.60"))
                .add(audience.multiply(new BigDecimal("0.25")))
                .add(trend.multiply(new BigDecimal("0.15")));
        return new RecommendationItemResponse(
                "AUTHOR",
                candidate.authorId(),
                0,
                toPercent(score),
                similarAuthorReasonCode(category, audience, trend),
                similarAuthorReasonText(category, audience, trend),
                Map.of("displayName", value(candidate.displayName()))
        );
    }

    private List<RecommendationItemResponse> rank(List<RecommendationItemResponse> items) {
        return IntStream.range(0, items.size())
                .mapToObj(index -> {
                    RecommendationItemResponse item = items.get(index);
                    return new RecommendationItemResponse(
                            item.itemType(),
                            item.itemId(),
                            index + 1,
                            item.score(),
                            item.reasonCode(),
                            item.reasonText(),
                            item.metadata()
                    );
                })
                .toList();
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

    private BigDecimal max(List<BigDecimal> scores) {
        return scores.stream()
                .filter(score -> score != null && score.compareTo(BigDecimal.ZERO) > 0)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal normalize(BigDecimal value, BigDecimal max) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0 || max.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return clamp01(value.divide(max, 6, RoundingMode.HALF_UP));
    }

    private BigDecimal freshnessScore(Instant updatedAt) {
        if (updatedAt == null) {
            return BigDecimal.ZERO;
        }
        long ageDays = Duration.between(updatedAt, clock.instant()).toDays();
        if (ageDays < 0) {
            return BigDecimal.ONE;
        }
        if (ageDays >= 30) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(30 - ageDays).divide(new BigDecimal("30"), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal tagsOverlap(String sourceTags, String candidateTags) {
        Set<String> source = parseTags(sourceTags);
        Set<String> candidate = parseTags(candidateTags);
        if (source.isEmpty() || candidate.isEmpty()) {
            return BigDecimal.ZERO;
        }
        long intersection = candidate.stream().filter(source::contains).count();
        long union = java.util.stream.Stream.concat(source.stream(), candidate.stream()).distinct().count();
        return BigDecimal.valueOf(intersection).divide(BigDecimal.valueOf(union), 6, RoundingMode.HALF_UP);
    }

    private Set<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    private BigDecimal durationSimilarity(Integer sourceDuration, Integer candidateDuration) {
        if (sourceDuration == null || candidateDuration == null || sourceDuration <= 0 || candidateDuration <= 0) {
            return BigDecimal.ZERO;
        }
        int max = Math.max(sourceDuration, candidateDuration);
        int diff = Math.abs(sourceDuration - candidateDuration);
        return clamp01(BigDecimal.ONE.subtract(BigDecimal.valueOf(diff).divide(BigDecimal.valueOf(max), 6, RoundingMode.HALF_UP)));
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal clamp01(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }
        return value;
    }

    private BigDecimal toPercent(BigDecimal score) {
        return clamp01(score).multiply(new BigDecimal("100")).setScale(4, RoundingMode.HALF_UP);
    }

    private boolean equalsNullable(String left, String right) {
        return left != null && left.equals(right);
    }

    private Object value(Object value) {
        return value == null ? "" : value;
    }

    private String playlistReasonCode(BigDecimal category, BigDecimal author, BigDecimal popularity, BigDecimal quality, BigDecimal freshness) {
        if (category.compareTo(BigDecimal.ZERO) > 0 && category.compareTo(author) >= 0) {
            return "PLAYLIST_TOP_CATEGORY";
        }
        if (author.compareTo(BigDecimal.ZERO) > 0) {
            return "PLAYLIST_TOP_AUTHOR";
        }
        if (popularity.compareTo(BigDecimal.ZERO) > 0) {
            return "PLAYLIST_POPULAR";
        }
        if (quality.compareTo(freshness) >= 0) {
            return "PLAYLIST_QUALITY";
        }
        return "PLAYLIST_FRESH";
    }

    private String playlistReasonText(BigDecimal category, BigDecimal author, BigDecimal popularity, BigDecimal quality, BigDecimal freshness) {
        return switch (playlistReasonCode(category, author, popularity, quality, freshness)) {
            case "PLAYLIST_TOP_CATEGORY" -> "Плейлист совпадает с интересными пользователю категориями";
            case "PLAYLIST_TOP_AUTHOR" -> "Плейлист содержит авторов из профиля интересов пользователя";
            case "PLAYLIST_POPULAR" -> "Плейлист популярен за последнюю неделю";
            case "PLAYLIST_QUALITY" -> "Плейлист содержит подкасты с хорошими реакциями";
            default -> "Плейлист недавно обновлялся";
        };
    }

    private String similarPodcastReasonCode(BigDecimal category, BigDecimal author, BigDecimal tags, BigDecimal duration, BigDecimal popularity) {
        if (category.compareTo(BigDecimal.ZERO) > 0) {
            return "SAME_CATEGORY";
        }
        if (author.compareTo(BigDecimal.ZERO) > 0) {
            return "SAME_AUTHOR";
        }
        if (tags.compareTo(BigDecimal.ZERO) > 0) {
            return "TAGS_OVERLAP";
        }
        if (duration.compareTo(BigDecimal.ZERO) > 0) {
            return "SIMILAR_DURATION";
        }
        return popularity.compareTo(BigDecimal.ZERO) > 0 ? "POPULAR_SIMILAR" : "SIMILAR_FALLBACK";
    }

    private String similarPodcastReasonText(BigDecimal category, BigDecimal author, BigDecimal tags, BigDecimal duration, BigDecimal popularity) {
        return switch (similarPodcastReasonCode(category, author, tags, duration, popularity)) {
            case "SAME_CATEGORY" -> "Похожий подкаст из той же категории";
            case "SAME_AUTHOR" -> "Похожий подкаст того же автора";
            case "TAGS_OVERLAP" -> "Похожий подкаст с пересекающимися тегами";
            case "SIMILAR_DURATION" -> "Похожий подкаст с близкой длительностью";
            case "POPULAR_SIMILAR" -> "Популярный подкаст среди похожих кандидатов";
            default -> "Похожий подкаст из доступных опубликованных кандидатов";
        };
    }

    private String similarAuthorReasonCode(BigDecimal category, BigDecimal audience, BigDecimal trend) {
        if (audience.compareTo(BigDecimal.ZERO) > 0) {
            return "SHARED_AUDIENCE";
        }
        if (category.compareTo(BigDecimal.ZERO) > 0) {
            return trend.compareTo(BigDecimal.ZERO) > 0 ? "FALLBACK_CATEGORY_TREND" : "SHARED_CATEGORIES";
        }
        return "TRENDING_AUTHOR";
    }

    private String similarAuthorReasonText(BigDecimal category, BigDecimal audience, BigDecimal trend) {
        return switch (similarAuthorReasonCode(category, audience, trend)) {
            case "SHARED_AUDIENCE" -> "Автора слушают пользователи с похожими интересами";
            case "FALLBACK_CATEGORY_TREND" -> "Автор из похожих категорий с высоким трендом";
            case "SHARED_CATEGORIES" -> "Автор публикует подкасты в похожих категориях";
            default -> "Автор с высоким текущим трендом";
        };
    }
}
