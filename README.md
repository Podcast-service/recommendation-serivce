# Recommendation Service

`Recommendation Service` — самостоятельный Spring Boot микросервис для будущих персональных рекомендаций подкастов, рекомендаций плейлистов, похожих подкастов и авторов, трендов, рейтингов, user interest profiles, агрегатов активности и recommendation cache.

Текущий этап намеренно не содержит бизнес-логики рекомендаций и Kafka listeners. Сервис не читает БД Podcast Core напрямую и должен стартовать независимо от Podcast Core и Kafka.

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

## Локальный запуск

Поднять пустую локальную PostgreSQL БД:

```powershell
docker compose up -d
```

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
- `RECOMMENDATION_REFRESH_JOB_ENABLED=false`
- `RECOMMENDATION_GLOBAL_JOB_ENABLED=false`
- `RECOMMENDATION_CACHE_CLEANUP_ENABLED=false`

Текущее безопасное решение: Kafka consumer для content events объявлен, но не создаётся при дефолтном `RECOMMENDATION_KAFKA_CONSUMERS_ENABLED=false`. Consumers должны обрабатывать только версионированные events и быть идемпотентными через `processed_events`.

## Kafka Event Contracts

DTO для recommendation events совместимы с текущей реализацией Podcast Core из ветки `connect_outbox`. Поддерживаются только версионированные `eventType` формата `.v1`:

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

Текущая ветка Podcast Core `kafka_publisher` публикует activity payload без `categoryId`, `authorId` и `progressPercent` для podcast activity events. Recommendation Service поддерживает эти поля как optional backward-compatible расширение: если `categoryId` или `authorId` отсутствует, соответствующая часть profile/stat update пропускается с warn log, а остальная обработка события продолжается.

Метрики:

- `recommendation.events.processed`
- `recommendation.events.duplicates`
- `recommendation.events.failed`

## Схема БД

Если архитектурные SQL-файлы отсутствуют в репозитории, Flyway-схема сохраняет документированные имена read-model таблиц, использует `timestamp with time zone` для timestamp-полей и хранит cache payload как `text` до фиксации recommendation payload contracts на отдельном feature-flagged этапе.

## Независимость

Сервис владеет собственной схемой БД и никогда не подключается напрямую к БД Podcast Core. Podcast Core не должен зависеть от доступности Recommendation Service для пользовательских действий.
