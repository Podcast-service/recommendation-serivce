package recommendationService.recommendation.scoring;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class SimilarityScorer {

    public BigDecimal podcastScore(
            BigDecimal category,
            BigDecimal author,
            BigDecimal tags,
            BigDecimal duration,
            BigDecimal popularity
    ) {
        return percent(category.multiply(new BigDecimal("0.40"))
                .add(author.multiply(new BigDecimal("0.25")))
                .add(tags.multiply(new BigDecimal("0.20")))
                .add(duration.multiply(new BigDecimal("0.10")))
                .add(popularity.multiply(new BigDecimal("0.05"))));
    }

    public BigDecimal authorScore(BigDecimal category, BigDecimal audience, BigDecimal trend) {
        return percent(category.multiply(new BigDecimal("0.60"))
                .add(audience.multiply(new BigDecimal("0.25")))
                .add(trend.multiply(new BigDecimal("0.15"))));
    }

    public boolean podcastCandidate(
            String sourceAuthor,
            String sourceCategory,
            String sourceTags,
            Integer sourceDuration,
            String candidateAuthor,
            String candidateCategory,
            String candidateTags,
            Integer candidateDuration
    ) {
        return equalsNullable(sourceCategory, candidateCategory)
                || equalsNullable(sourceAuthor, candidateAuthor)
                || tagsOverlap(sourceTags, candidateTags).compareTo(BigDecimal.ZERO) > 0
                || durationSimilarity(sourceDuration, candidateDuration).compareTo(new BigDecimal("0.70")) >= 0;
    }

    public BigDecimal tagsOverlap(String sourceTags, String candidateTags) {
        Set<String> source = parseTags(sourceTags);
        Set<String> candidate = parseTags(candidateTags);
        if (source.isEmpty() || candidate.isEmpty()) {
            return BigDecimal.ZERO;
        }
        long intersection = candidate.stream().filter(source::contains).count();
        long union = java.util.stream.Stream.concat(source.stream(), candidate.stream()).distinct().count();
        return BigDecimal.valueOf(intersection).divide(BigDecimal.valueOf(union), 6, RoundingMode.HALF_UP);
    }

    public BigDecimal durationSimilarity(Integer sourceDuration, Integer candidateDuration) {
        if (sourceDuration == null || candidateDuration == null || sourceDuration <= 0 || candidateDuration <= 0) {
            return BigDecimal.ZERO;
        }
        int max = Math.max(sourceDuration, candidateDuration);
        int diff = Math.abs(sourceDuration - candidateDuration);
        return BigDecimal.ONE.subtract(BigDecimal.valueOf(diff).divide(BigDecimal.valueOf(max), 6, RoundingMode.HALF_UP))
                .max(BigDecimal.ZERO)
                .min(BigDecimal.ONE);
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

    private BigDecimal percent(BigDecimal value) {
        return value.max(BigDecimal.ZERO)
                .min(BigDecimal.ONE)
                .multiply(new BigDecimal("100"))
                .setScale(4, RoundingMode.HALF_UP);
    }

    private boolean equalsNullable(String left, String right) {
        return left != null && left.equals(right);
    }
}
