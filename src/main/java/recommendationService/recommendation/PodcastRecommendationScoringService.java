package recommendationService.recommendation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;

@Service
public class PodcastRecommendationScoringService {

    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 100;
    private static final int PROFILE_LIMIT = 20;
    private static final int CANDIDATE_POOL_MIN = 500;

    private final PodcastRecommendationQueryRepository repository;
    private final Clock clock;

    public PodcastRecommendationScoringService(PodcastRecommendationQueryRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<PodcastRecommendationResponse> calculatePodcasts(
            String userId,
            Integer limit,
            String categoryId,
            Boolean excludeSeen
    ) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }

        int normalizedLimit = normalizeLimit(limit);
        boolean shouldExcludeSeen = excludeSeen == null || excludeSeen;
        List<InterestScore> categoryInterests = repository.findTopCategoryInterests(userId, PROFILE_LIMIT);
        List<InterestScore> authorInterests = repository.findTopAuthorInterests(userId, PROFILE_LIMIT);
        Map<String, BigDecimal> categoryScores = normalizeInterestScores(categoryInterests);
        Map<String, BigDecimal> authorScores = normalizeInterestScores(authorInterests);

        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        Instant freshPublishedAfter = clock.instant().minus(Duration.ofDays(30));
        List<PodcastRecommendationCandidate> candidates = repository.findCandidates(
                userId,
                categoryInterests.stream().map(InterestScore::itemId).toList(),
                authorInterests.stream().map(InterestScore::itemId).toList(),
                categoryId,
                today.minusDays(6),
                today,
                freshPublishedAfter,
                Math.max(CANDIDATE_POOL_MIN, normalizedLimit * 20)
        );

        BigDecimal maxPopularity = candidates.stream()
                .map(PodcastRecommendationCandidate::popularityScore)
                .filter(score -> score != null && score.compareTo(BigDecimal.ZERO) > 0)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
        boolean emptyProfile = categoryInterests.isEmpty() && authorInterests.isEmpty();

        List<ScoredPodcastRecommendation> scored = candidates.stream()
                .filter(candidate -> !candidate.disliked())
                .filter(candidate -> !shouldExcludeSeen || !isSeen(candidate))
                .map(candidate -> score(candidate, categoryScores, authorScores, maxPopularity, emptyProfile, shouldExcludeSeen))
                .filter(candidate -> candidate.normalizedScore().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(ScoredPodcastRecommendation::normalizedScore).reversed()
                        .thenComparing(candidate -> candidate.candidate().podcastId()))
                .limit(normalizedLimit)
                .toList();

        return IntStream.range(0, scored.size())
                .mapToObj(index -> toResponse(scored.get(index), index + 1))
                .toList();
    }

    public boolean hasPositiveProfile(String userId) {
        return !repository.findTopCategoryInterests(userId, 1).isEmpty()
                || !repository.findTopAuthorInterests(userId, 1).isEmpty();
    }

    private ScoredPodcastRecommendation score(
            PodcastRecommendationCandidate candidate,
            Map<String, BigDecimal> categoryScores,
            Map<String, BigDecimal> authorScores,
            BigDecimal maxPopularity,
            boolean emptyProfile,
            boolean excludeSeen
    ) {
        BigDecimal categoryScore = scoreFor(categoryScores, candidate.categoryId());
        BigDecimal authorScore = scoreFor(authorScores, candidate.authorId());
        BigDecimal popularityScore = normalize(candidate.popularityScore(), maxPopularity);
        BigDecimal freshnessScore = freshnessScore(candidate);
        BigDecimal qualityScore = qualityScore(candidate);
        BigDecimal diversityScore = diversityScore(categoryScore, authorScore);

        BigDecimal weighted = categoryScore.multiply(new BigDecimal("0.35"))
                .add(authorScore.multiply(new BigDecimal("0.25")))
                .add(popularityScore.multiply(new BigDecimal("0.20")))
                .add(freshnessScore.multiply(new BigDecimal("0.10")))
                .add(qualityScore.multiply(new BigDecimal("0.05")))
                .add(diversityScore.multiply(new BigDecimal("0.05")));

        BigDecimal alreadySeenPenalty = !excludeSeen && isSeen(candidate) ? new BigDecimal("0.25") : BigDecimal.ZERO;
        BigDecimal dislikedPenalty = candidate.disliked() ? BigDecimal.ONE : BigDecimal.ZERO;
        BigDecimal normalized = clamp01(weighted.subtract(alreadySeenPenalty).subtract(dislikedPenalty))
                .multiply(new BigDecimal("100"))
                .setScale(4, RoundingMode.HALF_UP);

        return new ScoredPodcastRecommendation(
                candidate,
                normalized,
                reasonCode(emptyProfile, categoryScore, authorScore, popularityScore, freshnessScore),
                reasonText(emptyProfile, categoryScore, authorScore, popularityScore, freshnessScore)
        );
    }

    private PodcastRecommendationResponse toResponse(ScoredPodcastRecommendation scored, int rank) {
        PodcastRecommendationCandidate candidate = scored.candidate();
        return new PodcastRecommendationResponse(
                candidate.podcastId(),
                rank,
                scored.normalizedScore(),
                scored.reasonCode(),
                scored.reasonText(),
                Map.of(
                        "title", value(candidate.title()),
                        "authorId", value(candidate.authorId()),
                        "categoryId", value(candidate.categoryId()),
                        "publishedAt", value(candidate.publishedAt())
                )
        );
    }

    private Map<String, BigDecimal> normalizeInterestScores(List<InterestScore> interests) {
        BigDecimal max = interests.stream()
                .map(InterestScore::score)
                .filter(score -> score != null && score.compareTo(BigDecimal.ZERO) > 0)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
        if (max.compareTo(BigDecimal.ZERO) <= 0) {
            return Map.of();
        }
        return interests.stream()
                .collect(Collectors.toMap(
                        InterestScore::itemId,
                        interest -> clamp01(interest.score().divide(max, 6, RoundingMode.HALF_UP)),
                        (left, right) -> left
                ));
    }

    private BigDecimal qualityScore(PodcastRecommendationCandidate candidate) {
        if (candidate.ratingCount() > 0) {
            BigDecimal averageRating = candidate.ratingSum()
                    .divide(BigDecimal.valueOf(candidate.ratingCount()), 6, RoundingMode.HALF_UP);
            return clamp01(averageRating.divide(new BigDecimal("5"), 6, RoundingMode.HALF_UP));
        }
        long reactions = candidate.likeCount() + candidate.dislikeCount();
        if (reactions > 0) {
            return BigDecimal.valueOf(candidate.likeCount())
                    .divide(BigDecimal.valueOf(reactions), 6, RoundingMode.HALF_UP);
        }
        return new BigDecimal("0.5");
    }

    private BigDecimal freshnessScore(PodcastRecommendationCandidate candidate) {
        if (candidate.publishedAt() == null) {
            return BigDecimal.ZERO;
        }
        long ageDays = Duration.between(candidate.publishedAt(), clock.instant()).toDays();
        if (ageDays < 0) {
            return BigDecimal.ONE;
        }
        if (ageDays >= 30) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(30 - ageDays)
                .divide(new BigDecimal("30"), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal diversityScore(BigDecimal categoryScore, BigDecimal authorScore) {
        return categoryScore.compareTo(BigDecimal.ZERO) > 0 && authorScore.compareTo(BigDecimal.ZERO) > 0
                ? new BigDecimal("0.5")
                : BigDecimal.ONE;
    }

    private String reasonCode(
            boolean emptyProfile,
            BigDecimal categoryScore,
            BigDecimal authorScore,
            BigDecimal popularityScore,
            BigDecimal freshnessScore
    ) {
        if (emptyProfile) {
            return "FALLBACK_POPULAR";
        }
        if (categoryScore.compareTo(BigDecimal.ZERO) > 0 && categoryScore.compareTo(authorScore) >= 0) {
            return "TOP_CATEGORY";
        }
        if (authorScore.compareTo(BigDecimal.ZERO) > 0) {
            return "TOP_AUTHOR";
        }
        if (popularityScore.compareTo(BigDecimal.ZERO) > 0 && popularityScore.compareTo(freshnessScore) >= 0) {
            return "POPULAR_NOW";
        }
        if (freshnessScore.compareTo(BigDecimal.ZERO) > 0) {
            return "NEW_RELEASE";
        }
        return "FALLBACK_POPULAR";
    }

    private String reasonText(
            boolean emptyProfile,
            BigDecimal categoryScore,
            BigDecimal authorScore,
            BigDecimal popularityScore,
            BigDecimal freshnessScore
    ) {
        return switch (reasonCode(emptyProfile, categoryScore, authorScore, popularityScore, freshnessScore)) {
            case "TOP_CATEGORY" -> "Рекомендовано по сильному интересу к категории";
            case "TOP_AUTHOR" -> "Рекомендовано по сильному интересу к автору";
            case "POPULAR_NOW" -> "Популярно среди слушателей за последнюю неделю";
            case "NEW_RELEASE" -> "Новый опубликованный выпуск";
            default -> "Популярная рекомендация без персонального профиля";
        };
    }

    int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }
        return limit;
    }

    private BigDecimal scoreFor(Map<String, BigDecimal> scores, String key) {
        if (key == null) {
            return BigDecimal.ZERO;
        }
        return scores.getOrDefault(key, BigDecimal.ZERO);
    }

    private BigDecimal normalize(BigDecimal value, BigDecimal max) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0 || max.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return clamp01(value.divide(max, 6, RoundingMode.HALF_UP));
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

    private boolean isSeen(PodcastRecommendationCandidate candidate) {
        return candidate.lastInteractionAt() != null
                || candidate.playFinishedCount() > 0
                || candidate.liked()
                || candidate.disliked();
    }

    private Object value(Object value) {
        return value == null ? "" : value;
    }

    private record ScoredPodcastRecommendation(
            PodcastRecommendationCandidate candidate,
            BigDecimal normalizedScore,
            String reasonCode,
            String reasonText
    ) {
    }
}
