# Operations

## Flags

| Flag | Default |
|---|---|
| `RECOMMENDATION_SECURITY_ENABLED` | `true` |
| `RECOMMENDATION_KAFKA_CONSUMERS_ENABLED` | `false` |
| `RECOMMENDATION_KAFKA_DLT_ENABLED` | `true` |
| `RECOMMENDATION_REFRESH_JOB_ENABLED` | `false` |
| `RECOMMENDATION_GLOBAL_JOB_ENABLED` | `false` |
| `RECOMMENDATION_CACHE_CLEANUP_ENABLED` | `false` |
| `RECOMMENDATION_PROMETHEUS_PUBLIC` | `false` |

## Metrics

- `recommendation.events.processed`
- `recommendation.events.duplicates`
- `recommendation.events.failed`
- `recommendation.events.dlt`
- `recommendation.cache.hit`
- `recommendation.cache.miss`
- `recommendation.cache.refresh.count`
- `recommendation.cache.cleanup.count`
- `recommendation.jobs.duration`

Kafka client metrics exported by Micrometer should be used for consumer lag dashboards. Alert on sustained lag, any growing DLT rate, cache refresh errors and job duration regression.

## DLT Handling

1. Disable consumers if DLT growth is continuous.
2. Inspect source topic, partition, offset and exception headers in the DLT record.
3. Fix code or data contract.
4. Deploy with consumers off, then replay selected DLT records to the original topic.
5. Re-enable consumers and monitor duplicates, snapshots and stats.

## Rollout

1. Deploy Recommendation Service with consumers and jobs off.
2. Verify health, JWT behavior and docs.
3. Enable consumers in stage and inspect `processed_events`, snapshots, stats, lag and DLT.
4. Enable cache refresh and cleanup jobs.
5. Roll out Core outbox flags gradually.

## Rollback

Disable consumers and jobs first. Recommendation APIs remain read-only and can continue serving existing cache/read models. Inspect DLT and replay events after the fix. No Podcast Core database rollback is required.
