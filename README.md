# Wallet / Payments Service

A production-shaped wallet & payments backend built with Spring Boot, developed phase-by-phase as a
hands-on tour of backend engineering — persistence, transactions, auth, caching, observability, and
a containerized deployment.

## Tech stack

| Area | Choice |
|------|--------|
| Language / runtime | Java 21 |
| Framework | Spring Boot 3.5 (Web, Data JPA, Security, Cache, Actuator) |
| Database | PostgreSQL 16, schema versioned with Flyway |
| Cache | Redis 7 |
| Auth | Stateless JWT + Spring Security |
| Observability | Micrometer + Prometheus, tracing, Actuator |
| Testing | JUnit 5, MockMvc, Testcontainers |
| Packaging / CI | Multi-stage Docker image, docker-compose, GitHub Actions |

## Getting started

Run the whole stack:

```bash
docker compose up -d --build
curl -s localhost:8080/actuator/health | jq .status
```

Run the app locally against containerized data services:

```bash
docker compose up -d postgres redis
./mvnw spring-boot:run
```

Test:

```bash
./mvnw test     # unit + slice + Testcontainers integration tests (needs Docker running)
```

## Local environment notes

The Docker containers are published on non-default host ports to avoid conflicts with local services:

| Service | Container port | Host port |
|---------|----------------|-----------|
| Postgres | 5432 | 5433 |
| Redis | 6379 | 6380 |

Inside the compose network, services reach each other by name on their internal ports.
