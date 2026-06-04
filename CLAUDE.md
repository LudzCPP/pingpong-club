# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Pingpong Club — a platform for managing table tennis training and a sports club. Two-tier app: **Spring Boot 3 REST API** (`src/`) + **React/Vite SPA** (`frontend/`).

## Commands

### Backend (Maven — run from `E:\projekt`)

```powershell
# Start the database (required before running the app or integration tests)
docker-compose up -d

# Build (skip tests)
mvn package -DskipTests

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=TrainingServiceTest

# Run a single test method
mvn test -Dtest=TrainingServiceTest#createTraining_nameAlwaysSetFromPlayerFirstName

# Start the backend
mvn spring-boot:run
```

### Frontend (npm — run from `E:\projekt\frontend`)

```powershell
npm install       # first time
npm run dev       # dev server at http://localhost:5173
npm run lint      # ESLint
npm run build     # production build
```

Backend serves at `http://localhost:8080/api`. Swagger UI: `http://localhost:8080/api/swagger-ui/index.html`.

## Architecture

### Backend package structure (`pl.pingpong.club`)

```
config/      SecurityConfig, JwtService, JwtAuthFilter, DataInitializer, OpenApiConfig
controller/  AuthController, TrainingController, UserController, FinanceController
dto/         Request/Response records (no logic)
exception/   ResourceNotFoundException, BusinessRuleException, GlobalExceptionHandler
mapper/      MapStruct interfaces: TrainingMapper, UserMapper, LeagueMatchMapper
model/       User (implements UserDetails), Training, LeagueMatch, Role enum, TrainingStatus enum
repository/  JPA repositories; TrainingRepository has a native PostgreSQL query existsConflictForCoach
service/     AuthService, TrainingService, UserService, FinanceService, UserDetailsServiceImpl
```

### Security flow

Every request passes through `JwtAuthFilter` (extends `OncePerRequestFilter`). It extracts the Bearer token, calls `JwtService.extractEmail`, loads the user via `UserDetailsService`, and sets the `SecurityContext`. Public paths (`/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`) skip authentication. Method-level access is enforced with `@PreAuthorize("hasRole('COACH')")` via `@EnableMethodSecurity` on `SecurityConfig`.

### Role model

| Role | Capabilities |
|---|---|
| COACH | Full read/write: trainings, users, finances, league matches |
| PLAYER | Read-only own trainings and own finances; 404 (not 403) for other players' data |

Public `POST /auth/register` always creates PLAYER. Only COACH can create another COACH via `POST /users/coaches`.

### Key business rules (enforced in services, not controllers)

- Training `name` is always `"trening " + player.getFirstName()` — never client-supplied.
- Coach conflict check uses a native SQL query on `trainings` table (interval overlap logic).
- Only `SCHEDULED` trainings can be edited or cancelled; only `SCHEDULED` can be completed.
- `COMPLETED` training `totalPrice = hourlyRate * durationMinutes / 60` — computed by `TrainingMapper`.
- PLAYER sees only own league matches/finances; `playerId` param is ignored and overridden to `caller.getId()`.

### Database

PostgreSQL 16 via Docker (`docker-compose.yml`). Schema managed by Flyway:
- `V2__create_users_and_trainings.sql` — users and trainings tables
- `V3__create_league_matches.sql` — league_matches table

`DataInitializer` seeds the default coach account (`trener@pingpong.pl` / `Coach123!`) on startup if it doesn't exist.

`ddl-auto: validate` — Hibernate validates schema against Flyway migrations; never auto-creates tables.

### Frontend

React 19 SPA with React Router 7. State: `AuthContext` stores `{ email, role }` in `localStorage` alongside the JWT. `src/api/client.js` (axios) injects `Authorization: Bearer <token>` on every request and redirects to `/login` on 401. Four pages: Login, Trainings, Players (COACH only), Finances.

## Testing

### Strategy

- **Service unit tests** (`@ExtendWith(MockitoExtension.class)`) — all repositories and mappers are mocked; no database needed.
- **Controller slice tests** (`@WebMvcTest`) — services mocked; security uses a `TestSecurityConfig` (in `src/test/`) that replaces `SecurityConfig` + `JwtAuthFilter`.

### @WebMvcTest setup pattern

`SecurityConfig` and `JwtAuthFilter` must be excluded from the scan and replaced with `TestSecurityConfig` to avoid JwtAuthFilter dependency issues and ensure `@EnableMethodSecurity` applies correctly:

```java
@WebMvcTest(
    value = XController.class,
    excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class,
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)
    }
)
@Import(TestSecurityConfig.class)
class XControllerTest {
    @MockBean XService service;
    // No @MockBean JwtService or UserDetailsServiceImpl needed
}
```

CSRF is re-enabled in the MockMvc test context even though production disables it. Add `.with(csrf())` to all `POST`, `PUT`, `PATCH`, and `DELETE` requests in controller tests.

Use `@WithMockUser(username = "coach@test.pl", roles = "COACH")` to set the security context.
