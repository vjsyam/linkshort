# LinkShort - Production-Ready Full-Stack URL Shortener

A scalable, full-stack URL shortening platform with JWT authentication, dual-target QR codes, analytics, password-protected links, rate limiting, and a cyber-themed responsive interface.

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Technology Stack](#technology-stack)
- [System Architecture](#system-architecture)
- [Prerequisites](#prerequisites)
- [Local Development Setup](#local-development-setup)
  - [1. Environment Configuration](#1-environment-configuration)
  - [2. Start the Backend](#2-start-the-backend)
  - [3. Start the Frontend](#3-start-the-frontend)
- [Docker Compose Setup](#docker-compose-setup)
- [Production Deployment Guide](#production-deployment-guide)
  - [Backend Deployment (Render, Railway, VPS)](#backend-deployment-render-railway-vps)
  - [Frontend Deployment (Vercel, Netlify)](#frontend-deployment-vercel-netlify)
  - [Environment Variables Reference](#environment-variables-reference)
- [QR Code System](#qr-code-system)
- [API Reference](#api-reference)
  - [Public Endpoints](#public-endpoints)
  - [Authenticated Endpoints](#authenticated-endpoints)
  - [API Usage Examples](#api-usage-examples)
- [Project Directory Structure](#project-directory-structure)
- [Troubleshooting](#troubleshooting)
- [License](#license)

---

## Overview

LinkShort is an end-to-end URL management system designed for reliable link redirection, click tracking, and link security. The backend is powered by Spring Boot 3.4 and Java 21+, utilizing Base62 encoding to eliminate collision risks and guarantee deterministic short identifiers. The frontend is built with React 18, Vite, Recharts, and custom CSS design tokens.

---

## Key Features

- URL Shortening: Deterministic Base62 encoding mapping database sequence IDs to short codes.
- Anonymous & Authenticated Usage: Unregistered users can shorten links immediately. Registered users can manage, disable, delete, and inspect links.
- Anonymous Link Claiming: Any links generated during an anonymous session can be automatically claimed when creating an account or logging in.
- Dual-Target QR Code Generation:
  - Destination Site QR: Encodes the final target URL directly for immediate mobile scanning.
  - Short Link QR: Encodes the short redirect URL to track click counts and metrics.
  - Cross-device scannability with automatic LAN IP resolution in development.
- Analytics & Click Tracking: Aggregated metrics including total clicks, browser breakdown, device classification, and referrers.
- Password Protection: Optional link-level password encryption using BCrypt.
- Expiration Controls: Configurable Time-To-Live (TTL) with automatic expiry enforcement.
- Per-IP Rate Limiting: In-memory sliding-window request throttling with dedicated protection on authentication routes.
- Anti-SSRF Validation: Protection against private subnet targeting, loopback requests, and cloud metadata queries (e.g., 169.254.169.254).
- Cyberpunk Design System: Pure CSS styling with custom CSS variables, glassmorphism cards, neon accents, and responsive layouts.

---

## Technology Stack

### Backend
- Language: Java 21 or Java 24
- Framework: Spring Boot 3.4
- Security: Spring Security 6 with stateless JWT authentication (HMAC-SHA256) and BCrypt hashing
- Persistence: Spring Data JPA, Hibernate
- Primary Storage: In-memory H2 (development) / PostgreSQL 16 (production)
- Cache & Rate Limiting: In-memory tracker / Redis 7 (optional production layer)
- QR Generation: ZXing (Zebra Crossing) 3.5.3 with High (H) error correction

### Frontend
- Library: React 18 (SPA)
- Build Tool: Vite 5
- HTTP Client: Axios with automatic JWT bearer interceptor and token recovery
- Data Visualization: Recharts
- Notifications: React Hot Toast
- Styles: Modern Vanilla CSS with design token variables

---

## System Architecture

```
[ Client Browser / Phone Camera ]
               │
               ▼
[ Vercel / Nginx Reverse Proxy ]
   │                     │
   │ (Static Assets / SPA)│ (API / Short Code Redirects)
   ▼                     ▼
[ React 18 Frontend ]   [ Spring Boot 3.4 Backend ]
                             │
            ┌────────────────┴────────────────┐
            ▼                                 ▼
   [ PostgreSQL / H2 DB ]             [ Redis Cache ]
```

---

## Prerequisites

Before starting, ensure your system has the following tools installed:

1. Java Development Kit (JDK): JDK 21 or later (`java -version`)
2. Apache Maven: Version 3.9 or later (`mvn -version`)
3. Node.js & npm: Node.js 18+ and npm 10+ (`node -v` and `npm -v`)
4. Docker & Docker Compose (Optional): Required only for containerized deployment

---

## Local Development Setup

### 1. Environment Configuration

Create a `.env` file in the root directory by copying `.env.example`:

```bash
cp .env.example .env
```

Ensure `JWT_SECRET` contains a secure random key of at least 32 characters:

```properties
JWT_SECRET=supersecretjwtkeyforlocaldevelopmentlinkshort32charsmin
APP_BASE_URL=auto
CORS_ORIGINS=http://localhost:5173,http://localhost:3000
```

### 2. Start the Backend

Open a terminal, navigate to the `backend` folder, and run:

On Linux / macOS:
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.arguments="--app.jwt.secret=supersecretjwtkeyforlocaldevelopmentlinkshort32charsmin"
```

On Windows (PowerShell):
```powershell
cd backend
$env:JWT_SECRET="supersecretjwtkeyforlocaldevelopmentlinkshort32charsmin"
mvn spring-boot:run "-Dspring-boot.run.arguments=--app.jwt.secret=supersecretjwtkeyforlocaldevelopmentlinkshort32charsmin"
```

The backend starts on port `8080`.
- Health/API base: `http://localhost:8080/api`
- In-memory H2 database runs automatically with zero configuration.

### 3. Start the Frontend

Open a second terminal, navigate to the `frontend` folder, install dependencies, and start Vite:

```bash
cd frontend
npm install
npm run dev
```

The frontend will be available at:
```
http://localhost:5173
```

Vite proxies `/api` and short code paths to `http://localhost:8080` automatically.

---

## Docker Compose Setup

To run the complete production-like stack locally with PostgreSQL and Redis:

```bash
# 1. Export required environment variables
export JWT_SECRET=your-secure-secret-key-at-least-32-chars
export DATABASE_PASSWORD=your_secure_db_password
export APP_BASE_URL=http://localhost

# 2. Build and launch containers
docker-compose up --build -d

# 3. View logs
docker-compose logs -f
```

Services started:
- Frontend (Nginx): `http://localhost:80`
- Backend (Spring Boot): `http://localhost:8080`
- Database (PostgreSQL 16): `localhost:5432`
- Cache (Redis 7): `localhost:6379`

To stop the containers:
```bash
docker-compose down
```

---

## Production Deployment Guide

### Backend Deployment (Render, Railway, VPS)

1. Root Directory: `backend`
2. Build Command: `mvn clean package -DskipTests`
3. Start Command: `java -jar target/linkshort-backend-1.0.0.jar`
4. Required Environment Variables on the Host:
   - `SPRING_PROFILES_ACTIVE`: `prod`
   - `DATABASE_URL`: `jdbc:postgresql://<db_host>:<db_port>/<db_name>`
   - `DATABASE_USER`: `<db_username>`
   - `DATABASE_PASSWORD`: `<db_password>`
   - `JWT_SECRET`: Random 32+ character secret
   - `APP_BASE_URL`: Public domain for short links (e.g. `https://linkshort17.vercel.app` or `https://linkshort-api.onrender.com`)
   - `CORS_ORIGINS`: Allowed web client domains (e.g. `https://linkshort17.vercel.app,http://localhost:5173`)

### Frontend Deployment (Vercel, Netlify)

1. Root Directory: `frontend`
2. Framework Preset: `Vite`
3. Build Command: `npm run build`
4. Output Directory: `dist`
5. Rewrite Rules (`vercel.json`):
   Ensure requests to `/api/*`, `/qr/*`, and short codes redirect to the backend service.

```json
{
  "rewrites": [
    {
      "source": "/api/(.*)",
      "destination": "https://your-backend-url.onrender.com/api/$1"
    },
    {
      "source": "/qr/(.*)",
      "destination": "https://your-backend-url.onrender.com/api/qr/$1"
    },
    {
      "source": "/([a-zA-Z0-9_-]+)",
      "destination": "https://your-backend-url.onrender.com/$1"
    },
    {
      "source": "/(.*)",
      "destination": "/index.html"
    }
  ]
}
```

---

## Environment Variables Reference

| Variable | Default (Dev) | Production Example | Description |
|---|---|---|---|
| `JWT_SECRET` | Required | `a8f3b...` (min 32 chars) | HMAC-SHA-256 signing secret for authentication tokens |
| `APP_BASE_URL` | `auto` | `https://linkshort17.vercel.app` | Base URL used when printing short links and generating QR codes |
| `CORS_ORIGINS` | `http://localhost:5173,http://localhost:3000` | `https://linkshort17.vercel.app` | Comma-separated list of origins permitted to communicate with the API |
| `DATABASE_URL` | `jdbc:h2:mem:linkshort` | `jdbc:postgresql://host:5432/dbname` | Database connection URL |
| `DATABASE_USER` | `sa` | `linkshort` | Database username |
| `DATABASE_PASSWORD` | empty | `secret_password` | Database password |
| `REDIS_HOST` | `localhost` | `redis` | Redis host for caching and distributed rate limiting |
| `REDIS_PORT` | `6379` | `6379` | Redis port |

---

## QR Code System

The application provides a flexible QR code generator:

1. Destination Site QR:
   Encodes the target website address directly into the QR matrix. When scanned by a phone camera, the device navigates straight to the destination website without passing through redirection proxies.
2. Short Link QR:
   Encodes the shortened link (e.g. `https://linkshort17.vercel.app/abc`). Scanning tracks the visit in link analytics before redirecting to the final destination.
3. Automatic LAN Detection:
   When `APP_BASE_URL` is set to `auto` in local development, the backend detects your host machine's Wi-Fi / Ethernet LAN IP (e.g. `http://192.168.0.x:8080`) so phones connected to the same network can test QR codes seamlessly.

---

## API Reference

### Public Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/shorten` | Shorten a single URL (anonymous or authenticated) |
| `POST` | `/api/shorten/bulk` | Bulk shorten up to 20 URLs |
| `GET` | `/{shortCode}` | Resolve short code and redirect with HTTP 302 Found |
| `POST` | `/{shortCode}/verify` | Verify password for password-protected links |
| `GET` | `/api/qr/{shortCode}` | Get QR code PNG image (`?target=short` or `?target=direct`) |
| `POST` | `/api/auth/register` | Register a new user account |
| `POST` | `/api/auth/login` | Authenticate and obtain JWT Bearer token |

### Authenticated Endpoints

Requires header `Authorization: Bearer <jwt_token>`.

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/urls/my` | Retrieve all short links created by the current user |
| `PATCH` | `/api/urls/{shortCode}/toggle` | Enable or disable a link |
| `DELETE` | `/api/urls/{shortCode}` | Permanently delete a link |
| `GET` | `/api/analytics/{shortCode}` | View click analytics, referrers, devices, and browsers |
| `POST` | `/api/urls/claim` | Claim anonymous links created prior to sign in |

### API Usage Examples

#### 1. Shorten a URL
```bash
curl -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -d '{
    "originalUrl": "https://github.com",
    "title": "GitHub Profile",
    "customAlias": "my-github",
    "expiryMinutes": 1440
  }'
```

Response:
```json
{
  "shortCode": "my-github",
  "shortUrl": "http://localhost:8080/my-github",
  "originalUrl": "https://github.com",
  "createdAt": "2026-09-22T15:00:00",
  "expiryDate": "2026-09-23T15:00:00",
  "clickCount": 0,
  "isActive": true,
  "hasPassword": false
}
```

#### 2. Get QR Code
```bash
# Destination site QR
curl -o qr-direct.png "http://localhost:8080/api/qr/my-github?target=direct"

# Short link tracking QR
curl -o qr-short.png "http://localhost:8080/api/qr/my-github?target=short"
```

#### 3. Register and Login
```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "Password123!",
    "displayName": "Jane Doe"
  }'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "Password123!"
  }'
```

---

## Project Directory Structure

```
linkshort/
├── .env.example                     # Environment variables template
├── .gitignore                       # Git ignore file
├── README.md                        # Documentation
├── docker-compose.yml               # Production multi-container orchestration
├── backend/                         # Spring Boot 3.4 Backend
│   ├── pom.xml                      # Maven dependencies
│   ├── Dockerfile                   # Multi-stage JDK build image
│   └── src/main/
│       ├── java/com/linkshort/
│       │   ├── LinkShortApplication.java
│       │   ├── config/              # Security and Web configuration
│       │   ├── controller/          # REST endpoints (URL, Auth, QR, Redirect)
│       │   ├── dto/                 # Request and response models
│       │   ├── exception/           # Exception handlers
│       │   ├── filter/              # JWT auth and rate-limiting filters
│       │   ├── model/               # JPA entities (User, UrlMapping, ClickEvent)
│       │   ├── repository/          # Spring Data JPA repositories
│       │   ├── service/             # Business logic (URL, Auth, QR, Analytics)
│       │   └── util/                # Base62 encoder, network, SSRF validator
│       └── resources/
│           └── application.yml      # Base, dev, and prod configurations
└── frontend/                        # React 18 / Vite Frontend
    ├── package.json                 # Frontend dependencies & scripts
    ├── vite.config.js               # Dev server and reverse proxy setup
    ├── vercel.json                  # Vercel deployment rewrites
    ├── nginx.conf                   # Production Nginx reverse proxy
    ├── Dockerfile                   # Nginx container image
    └── src/
        ├── api/                     # Axios API client
        ├── components/              # UI elements (Form, Card, Dashboard, Modal)
        ├── hooks/                   # useAuth and useShorten custom hooks
        ├── App.jsx                  # Main application component
        ├── App.css                  # UI styling
        └── index.css                # Global CSS design tokens
```

---

## Troubleshooting

### "JWT_SECRET must be configured" Error on Startup
- Cause: The application enforces strict secret checks to prevent insecure deployments.
- Solution: Ensure `JWT_SECRET` is set in your environment or passed to Maven:
  ```bash
  mvn spring-boot:run "-Dspring-boot.run.arguments=--app.jwt.secret=your-random-secret-key-at-least-32-chars"
  ```

### CORS Errors in Browser When Deployed
- Cause: The backend rejected requests because the origin was missing from `CORS_ORIGINS`.
- Solution: Add your frontend deployment domain to the `CORS_ORIGINS` environment variable on your backend host:
  ```env
  CORS_ORIGINS=https://linkshort17.vercel.app,http://localhost:5173
  ```

### QR Code Opens Wrong Address on Phone
- Cause: Defaulting to `localhost` makes the QR code unreachable from external devices.
- Solution: Leave `APP_BASE_URL=auto` so LinkShort auto-binds to your host's local network IP, or set `APP_BASE_URL` to your live domain (e.g., `https://linkshort17.vercel.app`).

### Render Cold-Start Delay (Free Tier)
- Cause: Render free tier services suspend after 15 minutes of inactivity.
- Solution: The initial request after inactivity may take 15 to 30 seconds while the container boots. Subsequent requests respond instantly.

---

## License

This project is licensed under the MIT License. See the LICENSE file for details.
