# REST API

All endpoints below require `Authorization: Bearer <jwt>`. JWT validation matches Podcast Core: `HS256`, issuer, `user_id`, `email`, `roles`, `exp`, optional `nbf`.

| Endpoint | Notes |
|---|---|
| `GET /recommendation/v1/podcasts?userId=&limit=&categoryId=&excludeSeen=` | Own user or `ADMIN` |
| `GET /recommendation/v1/feed?userId=&limit=` | Own user or `ADMIN`; combines podcasts and public playlists |
| `GET /recommendation/v1/playlists?userId=&limit=` | Own user or `ADMIN`; public playlists only |
| `GET /recommendation/v1/podcasts/{podcastId}/similar?limit=` | Published related candidates only |
| `GET /recommendation/v1/authors/{authorId}/similar?limit=` | Active authors only |
| `GET /recommendation/v1/trends/podcasts?period=day|week|month&categoryId=&limit=` | Published podcasts |
| `GET /recommendation/v1/trends/authors?period=day|week|month&limit=` | Author trends |
| `GET /recommendation/v1/trends/playlists?period=day|week|month&limit=` | Public non-deleted playlists |

Example:

```bash
curl -H "Authorization: Bearer $JWT" \
  "http://localhost:8083/recommendation/v1/podcasts?userId=$USER_ID&limit=20&excludeSeen=true"
```

Responses contain stable rank, normalized score, `reasonCode`, `reasonText` and basic snapshot metadata. Errors: `400` invalid request, `401` missing/invalid JWT, `403` foreign `userId`, `500` unexpected server error.

Public infrastructure endpoints: `/actuator/health`, `/actuator/info`, Swagger UI, `/v3/api-docs/**`. `/actuator/prometheus` is public only when `RECOMMENDATION_PROMETHEUS_PUBLIC=true`.

Browser origins are configured with comma-separated `RECOMMENDATION_CORS_ALLOWED_ORIGINS`. Only read-only `GET` and preflight `OPTIONS` methods are allowed.
