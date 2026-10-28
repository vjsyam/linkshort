# LinkShort Design Notes

## Goal

Give people a short link that opens fast, can expire, and cannot be abused. Keep the API simple enough that a new teammate can read it in one sitting.

## Architecture

```
React app  -->  REST API (Spring Boot)  -->  Redis (cache)
                       |
                       +-------------------->  PostgreSQL (source of truth)
```

The API has three layers:

1. **Controller** takes the request, checks the input, and returns the response.
2. **Service** holds the rules: code generation, expiry, password checks, cache use.
3. **Repository** talks to PostgreSQL.

Everything runs in Docker, so the same setup works on a laptop and on a server.

## Why Redis in front of PostgreSQL

A short link is written once and read many times. Each read only needs one lookup: code in, URL out. That is the exact job a key value cache does well.

- A cache hit skips the database completely, so redirects stay quick even when traffic spikes.
- PostgreSQL stays the source of truth, so a Redis restart loses nothing.
- On a cache miss the service reads PostgreSQL, then fills Redis for the next request.

Trade-off: a cached link can be stale for a short time. Expiry and edits must clear or update the cached entry. In this codebase, Redis caches URL mappings with the key prefix `url:` and a configurable TTL (`app.cache.ttl-minutes`, defaulting to 60 minutes). When a link is deleted via `deleteUrl()`, the cached key is explicitly evicted with `redisTemplate.delete()`. Expired or inactive links caught on database lookup are rejected with `UrlExpiredException` (HTTP 410) or `UrlNotFoundException` (HTTP 404). If Redis is disabled (`app.cache.enabled: false`) or unavailable, the application logs a warning and falls back transparently to PostgreSQL without failing client requests.

## Short codes

Each link row gets a numeric id from PostgreSQL. The id is encoded in Base62 (digits, lowercase, uppercase). This gives:

- Short codes that stay short for a long time
- No collisions, because ids are unique
- A cheap decode back to the id when needed

Trade-off: codes grow in order, so someone could guess neighbours. Password protected links cover the cases where that matters.

## Security choices

| Risk | What the design does |
|---|---|
| Stolen passwords | BCrypt hashes, never plain text |
| Forged sessions | Signed JWT checked on each protected request |
| Abuse and scraping | Rate limiting per client (sliding window per IP) |
| Private links | Optional password on a link (hashed with BCrypt) |
| Secrets in code | Keys come from environment settings |

## Data model

| Table | Main fields |
|---|---|
| `users` | `id`, `email`, `password`, `display_name`, `created_at` |
| `url_mappings` | `id`, `original_url`, `short_code`, `created_at`, `expiry_date`, `click_count`, `user_id`, `title`, `link_password`, `is_active` |
| `click_events` | `id`, `short_code`, `clicked_at`, `ip_address`, `user_agent`, `referer` |

## Request flow: opening a short link

1. Client calls `GET /{shortCode}`.
2. If the link is password-protected (`requiresPassword(shortCode)`), the controller returns HTTP 403 Forbidden with `{"passwordRequired": true}`, prompting the frontend to authenticate with `POST /{shortCode}/verify`.
3. Otherwise, the service checks Redis for key `url:{shortCode}`.
4. On a cache hit, the service returns the cached URL immediately.
5. On a cache miss, it reads PostgreSQL via `UrlRepository`, checks that the link is active and not expired, populates the Redis cache (TTL 60 minutes), and returns the destination URL.
6. A click event is recorded asynchronously in `click_events` and the denormalized `click_count` on `url_mappings` is incremented.
7. The controller returns an HTTP 302 Found redirect with the `Location` header pointing to the original URL.
8. An unknown or inactive short code returns HTTP 404 Not Found; an expired code returns HTTP 410 Gone.

## Bug we hit and fixed

After deployment, a signed out user created a link and never saw the result. The cause was the sign in requirement on the create endpoint: the request went through but the response did not return the short link. The fix lets signed out requests receive their shortened link: `SecurityConfig` permits `POST /api/shorten` and `POST /api/shorten/bulk` publicly via `.permitAll()`, and `UrlController` extracts `userId` optionally (returns `null` when unauthenticated). The backend creates the link, generates the Base62 code, and returns the full `ShortenResponse` with HTTP 201 Created. Users can later claim their anonymous links upon registration or login using `POST /api/urls/claim`.

Lesson: test the signed out path on the deployed site, not just on localhost.

## Testing

- Unit tests for Base62 encoding, decoding, and uniqueness (`Base62Test`) (JUnit 5)
- Unit tests for link expiry, Redis cache hit/miss paths, password hashing, and cache eviction (`UrlServiceTest`) (JUnit 5, Mockito)
- Spring Boot tests for registration, password hashing verification, bad login rejection, and JWT token protection on authenticated endpoints (`AuthSecurityTest`) (Spring Boot Test, MockMvc)

## Future work

- Integration tests against real PostgreSQL and Redis (Testcontainers)
- A GitHub Actions workflow that runs the tests on every push
- Distributed Redis-backed rate limiting for multi-instance deployments
