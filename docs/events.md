# Kafka Events

## Topics

| Topic | Events | Kafka key |
|---|---|---|
| `podcast.activity.events.v1` | plays, likes, dislikes, author follows | Core uses `userId` for podcast activity and `authorId` for author activity |
| `podcast.content.events.v1` | podcast and playlist snapshots | `podcastId` or `playlistId` |
| `podcast.search.events.v1` | reserved future search events | reserved |

DLT topics append the configured `.DLT` destination: `podcast.activity.events.v1.DLT`, `podcast.content.events.v1.DLT`, `podcast.search.events.v1.DLT`.

## Envelope

```json
{
  "eventId": "uuid",
  "eventType": "podcast.liked.v1",
  "eventVersion": 1,
  "producer": "podcast-core",
  "occurredAt": "2026-06-01T12:00:00Z",
  "correlationId": null,
  "causationId": null,
  "userId": "uuid-or-null",
  "payload": {}
}
```

`eventId`, `eventType`, positive `eventVersion`, `producer`, `occurredAt` and `payload` are required. Unknown JSON fields are ignored.

## Payloads

| Event | Required payload fields | Optional/backward-compatible fields |
|---|---|---|
| `podcast.published.v1` | `podcastId`, `authorId`, `categoryId`, `title`, `publishedAt` | `description`, `durationSeconds`, `language`, `tags`, `status`, `isExplicit` |
| `podcast.updated.v1` | `podcastId`, `authorId`, `categoryId`, `title`, `updatedAt` | published fields including `publishedAt` |
| `podcast.deleted.v1` | `podcastId`, `deletedAt` | `authorId`, `categoryId`, `status` |
| `podcast.play_finished.v1` | `podcastId`, `userId` | `authorId`, `categoryId`, `durationSeconds`, `progressSeconds` or `listenedSeconds`, `progressPercent`, `source`, payload timestamps |
| `podcast.liked.v1`, `podcast.disliked.v1` | `podcastId`, `userId` | `authorId`, `categoryId`, payload timestamps |
| `author.followed.v1`, `author.unfollowed.v1` | `authorId`, `userId` | payload timestamps |
| `playlist.created.v1` | `playlistId`, `ownerUserId`, `title`, `createdAt` | `description`, `publicPlaylist` or `isPublic`, `podcastIds`, `visibility` |
| `playlist.updated.v1` | `playlistId`, `ownerUserId`, `title`, `updatedAt` | created fields |
| `playlist.deleted.v1` | `playlistId`, `deletedAt` | `ownerUserId`, `status` |

Podcast activity without author/category is enriched from local `podcast_catalog_snapshot`. Missing enrichment logs a warning but does not discard the usable part of the event.
