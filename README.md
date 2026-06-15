# Wallet / Payments Service

A production-shaped **wallet & payments backend** built with Spring Boot — money accounts, safe
transfers, authentication, caching, observability, and a containerized deployment. Built
phase-by-phase as a deep, practical tour of backend engineering with Spring Boot.

> **Domain:** Users → Accounts → Transfers → Ledger entries. Chosen because it naturally forces the
> hard, interview-favourite topics: transactions, concurrency, idempotency, optimistic locking,
> auth, caching, and observability.

---

## Tech stack

| Area | Choice |
|------|--------|
| Language / runtime | Java 21 |
| Framework | Spring Boot 3.5 (Web, Data JPA, Security, Cache, Actuator) |
| Database | PostgreSQL 16, schema versioned with **Flyway** |
| Cache | **Redis 7** (Spring Cache abstraction) |
| Auth | Stateless **JWT** (jjwt) + Spring Security, BCrypt passwords |
| Observability | Micrometer + Prometheus, Brave tracing, Actuator |
| Testing | JUnit 5, MockMvc, **Testcontainers** (real Postgres + Redis) |
| Packaging / CI | Multi-stage **Docker** image, docker-compose, GitHub Actions |

---

## Architecture

```
                 ┌──────────────────────────────────────────────────────────┐
   HTTP client ─▶│  Controller (HTTP, DTOs, @Valid, @PreAuthorize)          │
                 │      │                                                     │
                 │      ▼                                                     │
                 │  Service (@Transactional business logic, events)         │
                 │      │                 │                                   │
                 │      ▼                 ▼                                   │
                 │  Repository (JPA)   ApplicationEventPublisher            │
                 └──────┼─────────────────┼──────────────────────────────────┘
                        │                 │ AFTER_COMMIT + @Async
                        ▼                 ▼
                  PostgreSQL          TransferEventListener (metrics, notify)
                        ▲
              Redis cache (account views)        Filters: JwtAuthenticationFilter
              Actuator /health /prometheus       Cross-cutting: GlobalExceptionHandler
```

**Layered by responsibility:** controllers speak HTTP only; services own business rules and the
transaction boundary; repositories talk to the DB. DTOs (records) form the API contract so entities
never leak. One `@RestControllerAdvice` turns every failure into a uniform JSON error.

---

## Engineering highlights

- **Safe money movement** — idempotency keys (retries never double-spend), `@Version` optimistic
  locking, deadlock-safe lock ordering, and a double-entry ledger.
- **Stateless JWT security** — a request filter establishes identity from the token; `@PreAuthorize`
  enforces per-resource ownership ("you may only touch your own account"); login is anti-enumeration.
- **Distributed caching** — Redis-backed account reads with eviction on every write, so a balance is
  never served stale.
- **Async domain events** — `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`, so side effects
  (notifications, metrics) run off the request thread and only when the transaction actually commits.
- **Full observability** — health/readiness probes, Prometheus metrics (incl. custom business
  metrics), and trace ids in logs; `@Observed` yields a metric and a span from one annotation.
- **Real integration tests** — Testcontainers spins up actual Postgres + Redis; the full security
  chain and Flyway migrations are exercised end-to-end.
- **Performance pass** — tuned HikariCP, OSIV disabled, batchable ledger inserts (SEQUENCE ids),
  projection query on the auth hot path, composite indexes, container resource limits.

---

## The build, phase by phase

Each phase has a detailed write-up (flow diagram, file walkthrough, and an *interview lens*):

| # | Phase | Doc |
|---|-------|-----|
| 1 | Skeleton + first REST endpoint | [PHASE1.md](PHASE1.md) |
| 2 | Domain model + Postgres + Flyway | [PHASE2.md](PHASE2.md) |
| 3 | Service layer + DTOs + validation + error handling | [PHASE3.md](PHASE3.md) |
| 4 | Transactions + concurrency (safe transfers) | [PHASE4.md](PHASE4.md) |
| 5 | Auth — JWT + Spring Security | [PHASE5.md](PHASE5.md) |
| 6 | Testing — unit, slice, Testcontainers | [PHASE6.md](PHASE6.md) |
| 7 | Caching (Redis) + async events | [PHASE7.md](PHASE7.md) |
| 8 | Observability — Actuator, metrics, tracing | [PHASE8.md](PHASE8.md) |
| 9 | Docker + docker-compose + CI | [PHASE9.md](PHASE9.md) |
| 10 | Performance pass | [PHASE10.md](PHASE10.md) |

---

## API

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| `POST` | `/users` | public | Register a user (creates their first account) |
| `GET`  | `/users/{id}` | authenticated | User + their accounts |
| `POST` | `/auth/login` | public | Exchange credentials for a JWT |
| `GET`  | `/accounts/{id}` | owner | Account view (cached) |
| `POST` | `/accounts/{id}/deposit` | owner | Faucet deposit (demo) |
| `POST` | `/transfers` | owner of source | Idempotent transfer (`Idempotency-Key` header) |
| `GET`  | `/actuator/health` · `/info` · `/prometheus` | public | Ops endpoints |

Protected endpoints expect `Authorization: Bearer <token>`. Errors share one shape:
`{status, code, message, timestamp, errors[]}`.

---

## Getting started

### Run the whole stack (Docker)

```bash
docker compose up -d --build          # app + Postgres + Redis
curl -s localhost:8080/actuator/health | jq .status   # "UP"
docker compose down                   # stop (add -v to wipe the db volume)
```

### Run the app locally (against containerized data services)

```bash
docker compose up -d postgres redis   # data services only
./mvnw spring-boot:run                 # app on :8080
```

### Test

```bash
./mvnw test     # unit + slice + Testcontainers integration tests (needs Docker running)
```

### Try it

```bash
BASE=localhost:8080
curl -X POST $BASE/users -H 'Content-Type: application/json' \
  -d '{"email":"a@b.com","name":"Ada","password":"password123","currency":"USD"}'
TOKEN=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"a@b.com","password":"password123"}' | jq -r .accessToken)
curl $BASE/accounts/1 -H "Authorization: Bearer $TOKEN"
```

---

## Project layout

```
src/main/java/com/james/wallet/
  WalletApplication.java            entry point
  *Controller.java                  HTTP layer (auth, account, transfer, user)
  *Service.java                     business logic + transactions
  *Repository.java                  Spring Data JPA
  *.java (User, Account, Transfer,  entities + DTOs (records) + domain exceptions
          LedgerEntry, *Request,
          *Response, *Exception)
  SecurityConfig / JwtService /     security (Phase 5)
    JwtAuthenticationFilter / ...
  CacheConfig / AsyncConfig /        caching + events (Phase 7)
    TransferEventListener / ...
  ObservabilityConfig                metrics + tracing (Phase 8)
src/main/resources/
  application.properties
  db/migration/V1..V4__*.sql         Flyway migrations
src/test/java/com/james/wallet/      unit, slice, Testcontainers tests
Dockerfile · docker-compose.yml · .github/workflows/ci.yml
PHASE1.md … PHASE10.md               per-phase deep dives
```

---

## Local environment notes

This repo was developed on a Mac with conflicting local services, so the Docker containers are
published on non-default host ports:

| Service | Container port | Host port | Why |
|---------|----------------|-----------|-----|
| Postgres | 5432 | **5433** | host 5432 taken by Homebrew Postgres |
| Redis | 6379 | **6380** | host 6379 taken by Homebrew Redis |

Inside the compose network, services reach each other by name on their internal ports
(`postgres:5432`, `redis:6379`).

---

*Built as a hands-on Spring Boot masterclass — see the `PHASEn.md` files for the full reasoning
behind every decision.*
