# TTManager

A full-stack management platform for table tennis clubs — scheduling, player rosters, finances, and league results, built as a two-tier app with a Spring Boot 3 REST API and a React SPA.

## Features

**Scheduling**
- Training scheduling with automatic coach double-booking detection (interval-overlap check at the database level)
- Recurring training series (weekly, 2–12 weeks) with per-occurrence conflict checking and group cancel/reschedule
- Session packages — track remaining sessions per player, auto-decrement on completion, low-balance warnings
- Calendar view with monthly training counts

**AI-assisted input**
- Free-text or voice ("Web Speech API") training creation — a natural-language prompt like *"trening z Kubą jutro o 18"* is parsed by an LLM into a structured training (player, date/time, duration, price)
- Provider-agnostic: works against any OpenAI-compatible endpoint (Groq by default), configurable via env vars

**Accounts & access control**
- JWT authentication with refresh tokens, invite-based registration, and email-based password reset
- Three-tier role hierarchy (`ADMIN > COACH > PLAYER`) enforced with Spring Security method-level `@PreAuthorize`
- Multi-coach support with an admin dashboard (system-wide stats, coach/player moderation)
- "Virtual" players — a coach can track players who don't have an account of their own

**Finances & reporting**
- League match results and per-coach/per-player earnings summaries
- Reports page with aggregated stats

**Everything else**
- Transactional email notifications (training scheduled, join requests, password reset)
- Responsive, dark-themed UI with skeleton loaders, toasts, and animated transitions

## Tech stack

| | |
|---|---|
| **Backend** | Java 21 · Spring Boot 3 (Web, Security, Data JPA, Mail, Validation, Actuator) · PostgreSQL · Flyway · MapStruct · Lombok · JJWT |
| **Frontend** | React 19 · Vite · React Router 7 · Tailwind CSS 4 · Axios |
| **Testing** | JUnit 5 · Mockito · Spring Testcontainers · full integration suite (real Postgres via Testcontainers + `TestRestTemplate`) |
| **Docs / Infra** | springdoc-openapi (Swagger UI) · Docker · GitHub Actions |

## Architecture

```
config/      SecurityConfig, JwtService, JwtAuthFilter, DataInitializer, OpenApiConfig
controller/  REST controllers (Auth, Training, User, Admin, Finance, ...)
dto/         Request/response records — no logic
exception/   Domain exceptions + a global exception handler
mapper/      MapStruct interfaces (Training, User, LeagueMatch)
model/       JPA entities, Role enum, TrainingStatus enum
repository/  Spring Data repositories (a native query handles coach conflict detection)
service/     Business logic — all rules below live here, not in controllers
```

Every request passes through a `JwtAuthFilter` that extracts and validates the bearer token before Spring Security's filter chain runs. Access control is layered: coarse-grained role checks at the method level (`@PreAuthorize`), plus fine-grained ownership checks in the service layer — a `PLAYER` requesting another player's data gets a `404`, not a `403`, to avoid leaking which resources exist.

A few business rules worth calling out:
- A training's `name` is always derived server-side from the player's first name — never client-supplied.
- Only `SCHEDULED` trainings can be edited, cancelled, or completed; completing one computes `totalPrice` from the coach's rate and duration.
- Coach conflict detection runs as a native interval-overlap SQL query rather than in application code, so it's race-condition-safe under concurrent scheduling.
- Every recurring series shares a group UUID, so a single API call can cancel or reschedule "this one" vs. "the entire series."

## Getting started

**Prerequisites:** Java 21, Node 18+, Docker

```bash
# 1. Start PostgreSQL
docker-compose up -d

# 2. Backend — run from repo root
mvn spring-boot:run

# 3. Frontend — run from frontend/
npm install
npm run dev
```

The frontend runs at `http://localhost:5173`, the API at `http://localhost:8080/api`. Interactive API docs (Swagger UI) are served at `http://localhost:8080/api/swagger-ui/index.html`.

A default admin account is seeded on first run (see `DataInitializer`) so you can log in immediately without registering.

## Testing

```bash
mvn test
```

The suite covers service-layer business rules (Mockito, no database), controller slices (`@WebMvcTest` with a dedicated test security config), and a full end-to-end integration path against a real Postgres instance via Testcontainers.
