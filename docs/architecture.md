# Architecture

## Boundary

```text
Podcast Core domain transaction
  -> Core outbox_events
  -> Core outbox publisher
  -> Kafka topics
  -> Recommendation Service idempotent consumers
  -> Recommendation-owned PostgreSQL read models
  -> secured read-only API
  -> cache and optional refresh jobs
```

Recommendation Service never reads or writes the Podcast Core database and does not call Podcast Core over HTTP for scoring. Core business actions commit independently of Kafka and Recommendation Service availability.

## Packages

The existing codebase is kept as a focused layered layout:

| Package | Responsibility |
|---|---|
| `config`, `security`, `kafka` | Runtime configuration, JWT, listeners, retry and DLT |
| `events`, `events.payload` | Envelope, payload DTO and processed-event repository |
| `catalog` | Podcast and playlist snapshots built from content events |
| `profile`, `stats` | User interaction read models and daily aggregates |
| `recommendation`, `recommendation.scoring`, `trends` | Read-only query repositories, dedicated scorers, cache and jobs |
| `api` | REST error mapping |

Entities, Kafka payload DTO and REST response DTO remain separate. Controllers delegate to services. SQL stays in repository classes.

## Idempotency

Every consumer transaction begins with:

```sql
insert into processed_events(event_id, event_type, event_version, processed_at)
values (?, ?, ?, ?)
on conflict (event_id) do nothing;
```

If zero rows are inserted, the delivery is a duplicate and is acknowledged without side effects. If read-model mutation fails, the transaction rolls back together with `processed_events`, so Kafka retry can process the event again.

## Failure Modes

Unknown event types with a valid envelope are marked processed and ignored. Invalid envelope or payload, serialization failures and transient database failures are retried and then sent to DLT. Jobs are independent from consumers and API requests and are disabled by default.
