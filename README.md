# Recommendation Service

`Recommendation Service` — самостоятельный Spring Boot микросервис для будущих персональных рекомендаций подкастов, рекомендаций плейлистов, похожих подкастов и авторов, трендов, рейтингов, user interest profiles, агрегатов активности и recommendation cache.

Сервис содержит read-only API рекомендаций и трендов, cache-слой и выключаемые Kafka/jobs компоненты. Он не читает БД Podcast Core напрямую и должен стартовать независимо от Podcast Core и Kafka при дефолтных feature flags.

Подробная документация:

- [Архитектура](docs/architecture.md)
- [Kafka events](docs/events.md)
- [REST API](docs/api.md)
- [Operations](docs/operations.md)

## Стек

- Java 21
- Spring Boot
- Spring Web MVC
- Spring Data JPA
- PostgreSQL
- Flyway
- Spring Kafka
- Validation
- Actuator
- Micrometer
- SpringDoc OpenAPI
- Spring Security

## Локальный запуск

Поднять пустую локальную PostgreSQL БД:

```powershell
Copy-Item .env.example .env
docker compose up -d
```

`docker compose` читает настройки из локального `.env`, собирает и запускает приложение из `Dockerfile`. Kafka consumers остаются выключены по умолчанию, поэтому Kafka не требуется для локального старта.

Если дефолтные настройки не подходят, можно переопределить переменные окружения:

```powershell
$env:RECOMMENDATION_DB_URL='jdbc:postgresql://localhost:15433/recommendation_db'
$env:RECOMMENDATION_DB_USER='recommendation_user'
$env:RECOMMENDATION_DB_PASSWORD='recommendation_pass'
$env:RECOMMENDATION_SERVER_PORT='8083'
```

Команды проверки:

```powershell
.\gradlew.bat test
.\gradlew.bat bootRun
curl http://localhost:8083/actuator/health
```

Swagger UI по умолчанию выключен. Для локальной разработки его можно включить:

```powershell
$env:RECOMMENDATION_SWAGGER_ENABLED='true'
```

После этого UI будет доступен по адресу:

```text
http://localhost:8083/swagger
```

## Feature Flags

Все runtime-функции, которые могут менять поведение сервиса, выключены по умолчанию:

- `RECOMMENDATION_KAFKA_CONSUMERS_ENABLED=false`
- `RECOMMENDATION_SECURITY_ENABLED=true`
- `RECOMMENDATION_TRENDS_API_ENABLED=true`
- `RECOMMENDATION_PERSONAL_PODCASTS_API_ENABLED=true`
- `RECOMMENDATION_BLOCKS_API_ENABLED=true`
- `RECOMMENDATION_REFRESH_JOB_ENABLED=false`
- `RECOMMENDATION_GLOBAL_JOB_ENABLED=false`
- `RECOMMENDATION_CACHE_CLEANUP_ENABLED=false`

Текущее безопасное решение: Kafka consumer для content events объявлен, но не создаётся при дефолтном `RECOMMENDATION_KAFKA_CONSUMERS_ENABLED=false`. Consumers должны обрабатывать только версионированные events и быть идемпотентными через `processed_events`.

Read-only Trends API управляется `RECOMMENDATION_TRENDS_API_ENABLED`. По умолчанию он включён, чтобы сервис сразу удовлетворял contract readiness для чтения daily stats; при необходимости endpoint можно выключить без изменения схемы БД.

Personal Podcasts API управляется `RECOMMENDATION_PERSONAL_PODCASTS_API_ENABLED`. По умолчанию он включён как read-only endpoint поверх собственных read models Recommendation Service; при необходимости endpoint можно выключить без изменения схемы БД и без влияния на Kafka consumers.

Recommendation Blocks API управляется `RECOMMENDATION_BLOCKS_API_ENABLED`. Он добавляет read-only feed, playlist recommendations и similar endpoints; при необходимости блоки можно выключить отдельно от podcast recommendations.

Recommendation cache и periodic jobs выключены по умолчанию:

- `RECOMMENDATION_REFRESH_JOB_ENABLED=false`
- `RECOMMENDATION_GLOBAL_JOB_ENABLED=false`
- `RECOMMENDATION_CACHE_CLEANUP_ENABLED=false`

Jobs используют интервалы из `.env`: personal refresh `RECOMMENDATION_REFRESH_JOB_FIXED_DELAY_MS`, global refresh `RECOMMENDATION_GLOBAL_JOB_FIXED_DELAY_MS`, cleanup `RECOMMENDATION_CACHE_CLEANUP_FIXED_DELAY_MS`.

## Kafka Event Contracts

DTO для recommendation events совместимы с текущей outbox-реализацией Podcast Core. Поддерживаются версионированные `eventType` формата `.v1`:

- `podcast.published.v1`
- `podcast.updated.v1`
- `podcast.deleted.v1`
- `podcast.play_finished.v1`
- `podcast.liked.v1`
- `podcast.disliked.v1`
- `author.followed.v1`
- `author.unfollowed.v1`
- `playlist.created.v1`
- `playlist.updated.v1`
- `playlist.deleted.v1`

`RecommendationEventMapper` читает общий `DomainEventEnvelope`, игнорирует неизвестные JSON-поля и мапит `payload` в DTO по `eventType`. Неизвестный `eventType` не считается ошибкой десериализации: mapper возвращает envelope с raw JSON payload, чтобы будущий consumer мог безопасно проигнорировать событие. Невалидный envelope или payload считаются нарушением контракта.

Kafka listener для `podcast.content.events.v1` включается только через `RECOMMENDATION_KAFKA_CONSUMERS_ENABLED=true`. Он обрабатывает `podcast.*` и `playlist.*` content events, обновляет catalog snapshots и сохраняет `eventId` в `processed_events` в одной транзакции. Неизвестные события ack-safe игнорируются и также помечаются как processed, чтобы повторная доставка не блокировала consumer.

Kafka listener для `podcast.activity.events.v1` включается тем же feature flag. Он обрабатывает `podcast.play_finished.v1`, `podcast.liked.v1`, `podcast.disliked.v1`, `author.followed.v1`, `author.unfollowed.v1`, обновляет user profiles и daily stats, а затем сохраняет `eventId` в `processed_events` в той же транзакции.

Веса activity events:

- `podcast.play_finished.v1`: `+2.5` category, `+2.0` author
- `podcast.liked.v1`: `+3.0` category, `+2.5` author
- `podcast.disliked.v1`: `-2.0` category, `-1.5` author
- `author.followed.v1`: `+5.0` author
- `author.unfollowed.v1`: `-4.0` author

Recommendation Service сохраняет backward compatibility со старыми activity payload без `categoryId`, `authorId` и `progressPercent`. При отсутствии `categoryId` или `authorId` consumer сначала делает enrichment из собственного `podcast_catalog_snapshot`; если snapshot ещё не получен, доступная часть события обрабатывается, а пропущенное enrichment логируется как warning.

Метрики:

- `recommendation.events.processed`
- `recommendation.events.duplicates`
- `recommendation.events.failed`
- `recommendation.events.dlt`

Consumers используют manual ack, insert-first идемпотентность и retry/DLT strategy. После исчерпания retry сообщения публикуются в `podcast.activity.events.v1.DLT`, `podcast.content.events.v1.DLT` или `podcast.search.events.v1.DLT`.

## JWT Security

Все `/recommendation/v1/**` endpoints защищены Bearer JWT по тому же контракту, что и Podcast Core: `HS256`, shared secret, issuer `auth-service`, claims `user_id`, `email`, `roles`, optional `nbf`, clock skew 30 секунд. Для совместимости можно передать тот же secret, который Core получает как `PODCAST_ACCESS_TOKEN_SECRET`, через `RECOMMENDATION_JWT_SECRET`.

Публичные endpoints: `/actuator/health`, `/actuator/info`, Swagger UI и `/v3/api-docs/**`. Prometheus остаётся защищённым, пока явно не задано `RECOMMENDATION_PROMETHEUS_PUBLIC=true`.

Personal endpoints сравнивают query `userId` с JWT `user_id`. Чужие рекомендации доступны только роли `ADMIN`. Для local/dev security можно явно выключить через `RECOMMENDATION_SECURITY_ENABLED=false`; production default — `true`.

CORS origins задаются через `RECOMMENDATION_CORS_ALLOWED_ORIGINS`; разрешены только read-only `GET` и preflight `OPTIONS`.

## Trends API

REST API трендов:

- `GET /recommendation/v1/trends/podcasts?period=day|week|month&categoryId=&limit=`
- `GET /recommendation/v1/trends/authors?period=day|week|month&limit=`
- `GET /recommendation/v1/trends/playlists?period=day|week|month&limit=`

`limit` по умолчанию равен `50`, максимум `100`.

Безопасное решение по периодам: агрегация идёт по UTC calendar dates из daily stats tables.

- `day`: текущая UTC date
- `week`: текущая UTC date и предыдущие 6 dates
- `month`: текущая UTC date и предыдущие 29 dates

Podcast trends возвращают только snapshots со статусом `PUBLISHED`. `podcast.published.v1` и `podcast.updated.v1` обновляют podcast snapshot в статус `PUBLISHED`; `podcast.deleted.v1` оставляет tombstone со статусом `DELETED`.

## Personal Podcasts API

REST API персональных рекомендаций подкастов:

- `GET /recommendation/v1/podcasts?userId={userId}&limit=20&categoryId=&excludeSeen=true`

`limit` по умолчанию равен `20`, максимум `100`. `categoryId` ограничивает кандидатов одной категорией. При `excludeSeen=true` сервис не возвращает подкасты, по которым уже есть interaction для пользователя. Disliked podcasts не возвращаются независимо от `excludeSeen`.

Сервис не использует cache, ML-модель, HTTP calls в Podcast Core и прямое чтение БД Podcast Core. Кандидаты берутся только из собственных read models:

- top categories пользователя;
- top authors пользователя;
- popular podcasts за последние 7 UTC dates;
- fresh published podcasts за последние 30 дней;
- fallback на global popular при пустом профиле.

`recommendation_score` нормализуется в диапазон `0..100`, где `100` — лучший локальный кандидат в текущем response-контексте. MVP-формула:

```text
0.35 * category_match_score
+ 0.25 * author_match_score
+ 0.20 * popularity_score
+ 0.10 * freshness_score
+ 0.05 * quality_score
+ 0.05 * diversity_score
- already_seen_penalty
- disliked_penalty
```

Reason codes: `TOP_CATEGORY`, `TOP_AUTHOR`, `POPULAR_NOW`, `NEW_RELEASE`, `FALLBACK_POPULAR`.

API сначала читает `recommendation_cache`. При cache miss выполняется on-demand scoring и результат записывается обратно в cache. Если профиль пользователя пустой, API дополнительно может использовать `global_recommendation_cache`; при его отсутствии работает тот же on-demand fallback на global popular.

Cache entries хранят `generated_at`, `expires_at`, `item_rank`, `score`, `reason_code`, `reason_text`, `item_id`; `payload` содержит JSON с basic metadata. `item_rank` используется вместо SQL-колонки `rank`, чтобы не конфликтовать с зарезервированными словами и сохранить portable SQL для PostgreSQL/H2 tests.

Cache metrics:

- `recommendation.cache.hit`
- `recommendation.cache.miss`
- `recommendation.cache.refresh.count`
- `recommendation.cache.cleanup.count`

## Recommendation Blocks API

Дополнительные read-only блоки:

- `GET /recommendation/v1/feed?userId={userId}&limit=20`
- `GET /recommendation/v1/playlists?userId={userId}&limit=20`
- `GET /recommendation/v1/podcasts/{podcastId}/similar?limit=20`
- `GET /recommendation/v1/authors/{authorId}/similar?limit=20`

`limit` по умолчанию равен `20`, максимум `100`. Все scores нормализуются в диапазон `0..100`.

Feed объединяет `PODCAST` и `PLAYLIST` элементы и сортирует их по unified score. Для podcast части используется уже существующий cache-first personal podcast service.

Playlist scoring использует:

- совпадение категорий пользователя с опубликованными подкастами внутри playlist;
- совпадение авторов пользователя с опубликованными подкастами внутри playlist;
- популярность playlist за последние 7 UTC dates;
- качество подкастов внутри playlist по реакциям;
- freshness по `playlist_catalog_snapshot.updated_at`.

Similar podcasts formula:

```text
0.40 * same_category
+ 0.25 * same_author
+ 0.20 * tags_overlap
+ 0.10 * similar_duration
+ 0.05 * popularity_score
```

Для `tags_overlap` и `similar_duration` добавлены nullable поля `podcast_catalog_snapshot.tags` и `podcast_catalog_snapshot.duration_seconds`. Это безопасное backward-compatible расширение: старые snapshots без этих полей продолжают участвовать в рекомендациях, просто получают `0` за соответствующие компоненты.

Similar authors использует похожесть по категориям опубликованных подкастов, shared audience из `user_author_interest`, если данные есть, и fallback на авторов из тех же категорий с высоким trend score.

Recommendation Blocks API не читает БД Podcast Core, не вызывает Podcast Core по HTTP, не добавляет ML-модель и не меняет Kafka contracts.

## Схема БД

Если архитектурные SQL-файлы отсутствуют в репозитории, Flyway-схема сохраняет документированные имена read-model таблиц, использует `timestamp with time zone` для timestamp-полей и хранит cache payload как `text` до фиксации recommendation payload contracts на отдельном feature-flagged этапе.

## Независимость

Сервис владеет собственной схемой БД и никогда не подключается напрямую к БД Podcast Core. Podcast Core не должен зависеть от доступности Recommendation Service для пользовательских действий.
