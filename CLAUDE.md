# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

SpaceShift backend — a real-estate management REST API built with **Spring Boot 4.0.4** and **Java 21**. Domains: users/profiles, properties (`inmueble`), listings (`publicacion`), contracts, real-time chat, notifications, reports, a Stripe-backed credit/token system, and AI video processing via RunPod + AWS S3.

## Commands

The project uses the Maven wrapper (`mvnw` / `mvnw.cmd`). The app runs on **port 8081**.

```bash
./mvnw clean compile        # compile
./mvnw spring-boot:run      # run locally (port 8081)
./mvnw test                 # run all tests
./mvnw clean package        # build the executable JAR (target/api-0.0.1-SNAPSHOT.jar)

# Run a single test
./mvnw test -Dtest=ApiApplicationTests
./mvnw test -Dtest=ClassName#methodName
```

On Windows use `mvnw.cmd` (or `.\mvnw.cmd` in PowerShell) instead of `./mvnw`.

Swagger UI: `http://localhost:8081/swagger-ui/index.html` once running.

## Configuration & secrets

- Runtime config lives in `src/main/resources/application.properties`. The committed file currently contains **hardcoded credentials** (DB password, JWT secret, Stripe/Cloudinary/AWS/RunPod keys). The `spring-dotenv` dependency lets a root-level `.env` override these — see `.env.example` for the full variable list and the property each maps to.
- `spring.jpa.hibernate.ddl-auto=validate` — Hibernate **never** creates or alters tables. The schema is owned entirely by Flyway.

## Architecture

### Modular-by-domain structure

Code is organized by **business domain**, not by technical layer. Everything for one concept lives under `com.sw.api.modules.<domain>/`, each with its own `controller/`, `service/`, `model/`, `repository/`, `dto/` subpackages. To understand a feature, read one module folder rather than hunting across global `controllers/`/`services/` dirs.

Modules: `auth`, `usuario`, `inmueble`, `publicacion`, `contrato`, `chat`, `notificacion`, `reporte`, `token`, `video_processing`.

Cross-cutting code sits outside `modules/`:
- `shared/` — domain-agnostic infrastructure (`Auditable` base entity, `CloudinaryService`, `EmailService`). Imports nothing from any module.
- `security/` — JWT filter chain, `JwtService`, `SecurityConfig`, CORS.
- `config/` — Spring beans: `DatabaseSeeder`, `FirebaseConfig`, `WebSocketConfig`, `CloudinaryConfig`, `AuditConfig`, etc.

### Module dependency rule

Dependencies flow one direction only — lower-level modules never import higher-level ones. `shared` depends on nothing. `usuario`/`inmueble` depend only on `shared`. `publicacion` → `inmueble` + `usuario`. `chat`/`contrato` → `inmueble` + `publicacion` + `usuario`. `auth` is the only module that imports `security`. Preserve this direction when adding code; see `docs/migracion-arquitectura-modular.md` for the full map.

### Adding a new endpoint

The established flow (documented in `docs/crear_api.md`): write a Flyway migration first → JPA `@Entity` model (extend `Auditable` for created/updated tracking) → request/response DTOs (never expose entities directly) → `JpaRepository` → `@Service` (owns business logic + DTO↔entity mapping) → `@RestController` (thin, delegates to service). DTO mapping is done manually field-by-field, not with a mapper library.

### Database / Flyway

All schema changes go through versioned migrations in `src/main/resources/db/migration/` named `V<n>__description.sql` (currently up to V17). Never edit an already-applied migration — add a new one. `validate-on-migrate=false` is set, so checksum mismatches won't block startup, but treat applied migrations as immutable anyway.

Seed data: `DatabaseSeeder` (an `ApplicationRunner`) loads `db/seeders/initial-data.json` on startup. Gated by `app.seed.enabled` (defaults to enabled).

### Security model

Stateless JWT auth. `JwtAuthenticationFilter` runs before Spring's `UsernamePasswordAuthenticationFilter`. Route rules live in `SecurityConfig`:
- Public: `/api/auth/**`, Swagger, `/ws-chat/**`, `/api/webhooks/stripe`, `/api/tokens/paquetes`, and `GET` on `/api/publicaciones/**` and `/api/inmuebles/**`.
- `ROLE_ADMIN` only: `/api/usuarios/**`, `/api/reportes/**`.
- Everything else requires a valid token.

Tokens are issued/validated in `JwtService`. Roles are stored as `ROLE_*` authorities (see `usuario/enums/NombreRol`).

### Notable integrations

- **Chat** uses WebSocket/STOMP (`WebSocketConfig`, endpoint `/ws-chat`) plus Firebase Cloud Messaging for push (`FirebaseConfig` loads `firebase-service-account.json`).
- **Payments**: Stripe Checkout for buying credit packages; webhook at `/api/webhooks/stripe` (`StripeWebhookService`) confirms purchases. See `token` module.
- **Video processing**: uploads to AWS S3, submits jobs to a RunPod serverless endpoint, polls/stores result URLs. See `video_processing` module and `runpod.md`.
- **Cloudinary** hosts listing images.

## Conventions

- Code, comments, commit messages, and docs are in **Spanish**. Match this when editing.
- Lombok is used heavily (`@Data`, `@RequiredArgsConstructor`, `@Builder`) — prefer constructor injection via `@RequiredArgsConstructor` over field injection, as existing code does.
- Reference docs in `docs/` are written in Spanish and explain endpoint-creation, the modular migration, notifications, and enum design.
