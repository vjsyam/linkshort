# LinkShort

A secure URL shortener. Paste a long link, get a short one, and see how often it gets used.

Live site: https://linkshort17.vercel.app (Backend API: https://linkshort-chm3.onrender.com)

## What it does

- Turns long URLs into short Base62 codes
- Redirects fast, because hot links are served from Redis instead of PostgreSQL
- Lets links expire on a date you choose
- Tracks clicks (analytics)
- Makes a QR code for any short link
- Protects a link with a password
- Creates many links at once (bulk creation)
- Limits how often one client can call the API (rate limiting)

## Tech stack

| Layer | Tools |
|---|---|
| Backend | Java 21, Spring Boot 3.4, layered REST API |
| Frontend | React 18, Vite 5 |
| Data | PostgreSQL 16 (H2 for local dev), Redis 7 |
| Security | JWT (io.jsonwebtoken 0.12.6), BCrypt |
| Packaging | Docker, Docker Compose |
| Tests | JUnit 5, Mockito, Spring Boot Test |

## Getting started

### Requirements

- Java 21+
- Docker and Docker Compose
- Node.js 18+ (for the frontend)

### Run the backend

```bash
git clone https://github.com/vjsyam/linkshort.git
cd linkshort
cp .env.example .env        # fill in the values listed below
docker compose up --build
```

The API starts on http://localhost:8080.

### Run the frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend starts on http://localhost:5173.

### Settings

Put these in `.env`.

| Name | What it is |
|---|---|
| `DATABASE_URL` | PostgreSQL connection string (e.g., `jdbc:postgresql://localhost:5432/linkshort`) |
| `DATABASE_USER`, `DATABASE_PASSWORD` | Database credentials |
| `REDIS_HOST`, `REDIS_PORT` | Redis server address (e.g., `localhost`, `6379`) |
| `JWT_SECRET` | Secret key used to sign tokens (min 32 characters) |
| `APP_BASE_URL` | Base domain for short links (e.g., `http://localhost:8080` or `auto`) |
| `CORS_ORIGINS` | Permitted client origins (e.g., `http://localhost:5173,http://localhost:3000`) |

## Using the API

| Action | Request |
|---|---|
| Sign up | `POST /api/auth/register` with `email`, `password`, `displayName` |
| Log in | `POST /api/auth/login` with `email`, `password`, returns a JWT |
| Shorten a link | `POST /api/shorten` with `originalUrl`, optional `customAlias`, optional `expiryMinutes`, optional `title`, optional `password` |
| Bulk shorten | `POST /api/shorten/bulk` with `urls` (array of strings) |
| Open a link | `GET /{shortCode}` redirects (HTTP 302) to original URL (or HTTP 403 if password protected) |
| Verify password | `POST /{shortCode}/verify` with `password`, returns redirect URL |
| Get my links | `GET /api/urls/my` (requires token) |
| Toggle link active status | `PATCH /api/urls/{shortCode}/toggle` (requires token) |
| Delete a link | `DELETE /api/urls/{shortCode}` (requires token) |
| Claim anonymous links | `POST /api/urls/claim` with `shortCodes` array (requires token) |
| See click stats | `GET /api/analytics/{shortCode}` (requires token) |
| Get a QR code | `GET /api/qr/{shortCode}?target=short\|direct` |

Send the token as `Authorization: Bearer <token>`.

Example:

```bash
curl -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://example.com/a/very/long/path"}'
```

Example with authenticated request:

```bash
curl -X POST http://localhost:8080/api/shorten \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://example.com/a/very/long/path", "expiryMinutes": 1440, "title": "My Link"}'
```

## Running the tests

From the `backend` folder:

```bash
./mvnw test
```

(or on Windows: `mvnw.cmd test` or `mvn test`)

The tests cover Base62 code generation, link expiry, the Redis cache path, and authentication.

## Project layout

```
backend/    Spring Boot API (controller, service, repository layers)
frontend/   React app (Vite, Axios, Recharts)
docs/       Design notes (see DESIGN.md)
```

## Known limits

- Links are not shared across users unless you send them the short URL
- Short codes are generated sequentially from database IDs using Base62 (use link passwords for sensitive links)
- Default link expiry is 365 days when no explicit `expiryMinutes` is set
- In-memory rate limiting tracks sliding windows per IP; resets on application restart unless configured with distributed Redis
- Private network, loopback, and cloud metadata IPs (e.g., 169.254.169.254) are rejected by SSRF protection

## License

This project is licensed under the MIT License.
