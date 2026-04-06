# ⚡ LinkShort — Next-Gen URL Shortener

A production-ready, full-stack URL shortener with JWT auth, QR codes, analytics, password-protected links, and a futuristic cyberpunk UI.

![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4-6DB33F?logo=spring-boot)
![React](https://img.shields.io/badge/React-18-61DAFB?logo=react)
![License](https://img.shields.io/badge/License-MIT-blue)

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 🔗 URL Shortening | Base62 encoded short codes, no collisions |
| 📊 Rich Analytics | Click charts, browser/device/referrer breakdown, unique visitors |
| 🔐 JWT Authentication | Optional login — anonymous users can shorten, logged-in users manage |
| 🔒 Password Protection | Secure links with a password |
| 📱 Cross-Device QR Codes | Auto-detects LAN IP or uses custom domain |
| ⚡ Bulk Shortening | Shorten up to 20 URLs at once |
| 🔄 Link Management | Enable/disable, delete, search, title labels |
| ⏰ Link Expiry | Configurable TTL with visual expiry dates |
| 🛡️ Rate Limiting | Per-IP rate limiting (100 req/min default) |
| 🎨 Cyberpunk UI | Dark theme with neon accents, angular design, micro-animations |

---

## 🏗️ Tech Stack

- **Backend**: Java 21, Spring Boot 3.4, Spring Security, JPA/Hibernate
- **Frontend**: React 18, Vite, Recharts, Framer Motion
- **Database**: H2 (dev) / PostgreSQL (prod)
- **Cache**: Redis (optional, for production)
- **Auth**: Stateless JWT with BCrypt password hashing

---

## 🚀 Quick Start (Local Dev)

```bash
# Backend (runs on port 8080, uses H2 in-memory DB)
cd backend
mvn spring-boot:run

# Frontend (runs on port 5173)
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173** → start shortening URLs!

---

## 🐳 Docker Deployment (Production)

```bash
# Set your domain and secrets
export APP_BASE_URL=https://yourdomain.com
export JWT_SECRET=your-secure-secret-key-at-least-32-chars
export DATABASE_PASSWORD=your-secure-db-password

# Build and start everything
docker-compose up --build -d
```

The app will be available at **http://yourdomain.com** (port 80).

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `APP_BASE_URL` | `auto` (LAN IP) | Your domain for short links (e.g., `https://link.sh`) |
| `JWT_SECRET` | dev default | HMAC-SHA-256 secret for JWT tokens |
| `DATABASE_URL` | `jdbc:postgresql://db:5432/linkshort` | PostgreSQL connection string |
| `DATABASE_USER` | `linkshort` | DB username |
| `DATABASE_PASSWORD` | `linkshort_secret` | DB password |
| `REDIS_HOST` | `redis` | Redis hostname |

---

## 📐 API Reference

### Public Endpoints
| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/shorten` | Shorten a URL |
| `POST` | `/api/shorten/bulk` | Bulk shorten (up to 20) |
| `GET` | `/{shortCode}` | Redirect to original URL |
| `POST` | `/api/auth/register` | Register a new user |
| `POST` | `/api/auth/login` | Login (returns JWT) |

### Authenticated Endpoints (JWT required)
| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/urls/my` | Get user's links |
| `PATCH` | `/api/urls/{code}/toggle` | Enable/disable a link |
| `DELETE` | `/api/urls/{code}` | Delete a link |
| `GET` | `/api/analytics/{code}` | Get link analytics |
| `POST` | `/api/urls/claim` | Claim anonymous links |

---

## 🗂️ Project Structure

```
linkshort/
├── backend/                         # Spring Boot
│   ├── src/main/java/com/linkshort/
│   │   ├── controller/              # REST controllers
│   │   ├── service/                 # Business logic
│   │   ├── repository/              # JPA repositories
│   │   ├── model/                   # Entity classes
│   │   ├── dto/                     # Request/Response DTOs
│   │   ├── config/                  # Security, Redis config
│   │   ├── filter/                  # JWT auth filter
│   │   ├── exception/               # Custom exceptions
│   │   └── util/                    # Network utilities
│   └── Dockerfile
├── frontend/                        # React (Vite)
│   ├── src/
│   │   ├── components/              # UI components
│   │   ├── hooks/                   # useAuth, useShorten
│   │   ├── api/                     # Axios API client
│   │   ├── App.jsx                  # Main layout
│   │   ├── App.css                  # Component styles
│   │   └── index.css                # Design system
│   ├── nginx.conf                   # Production nginx
│   └── Dockerfile
└── docker-compose.yml               # Full stack orchestration
```

---

## 📜 License

MIT License — use it however you want.
