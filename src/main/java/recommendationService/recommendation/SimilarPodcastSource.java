package recommendationService.recommendation;

record SimilarPodcastSource(
        String podcastId,
        String authorId,
        String categoryId,
        String tags,
        Integer durationSeconds
) {
}
