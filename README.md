# Recommendation Service

`Recommendation Service` is a standalone Spring Boot microservice for future podcast recommendations, playlist recommendations, similar podcasts/authors, trends, ratings, user interest profiles, activity aggregates, and recommendation cache.

This initial stage intentionally contains no recommendation business logic and no Kafka listeners. The service does not read Podcast Core database and must start independently from Podcast Core and Kafka.

## Stack

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

## Local Run

Start an empty local PostgreSQL database:

```powershell
docker compose up -d
```

Or set environment variables if defaults are not suitable:

```powershell
$env:RECOMMENDATION_DB_URL='jdbc:postgresql://localhost:15433/recommendation_db'
$env:RECOMMENDATION_DB_USER='recommendation_user'
$env:RECOMMENDATION_DB_PASSWORD='recommendation_pass'
$env:RECOMMENDATION_SERVER_PORT='8083'
```

Run checks:

```powershell
.\gradlew.bat test
.\gradlew.bat bootRun
curl http://localhost:8083/actuator/health
```

Swagger UI is disabled by default and can be enabled for local development:

```powershell
$env:RECOMMENDATION_SWAGGER_ENABLED='true'
```

Then open:

```text
http://localhost:8083/swagger
```

## Feature Flags

All runtime features that can affect behavior are disabled by default:

- `RECOMMENDATION_KAFKA_CONSUMERS_ENABLED=false`
- `RECOMMENDATION_REFRESH_JOB_ENABLED=false`
- `RECOMMENDATION_GLOBAL_JOB_ENABLED=false`
- `RECOMMENDATION_CACHE_CLEANUP_ENABLED=false`

Current safe decision: Kafka infrastructure properties are present for future integration, but no `@KafkaListener` beans are defined in this stage. Future consumers must be versioned and idempotent through `processed_events`.

## Independence

The service owns its database schema and never connects directly to Podcast Core database. Podcast Core must not depend on Recommendation Service availability for user actions.
