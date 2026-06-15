package com.james.wallet;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for INTEGRATION tests — the top of the pyramid.
 *
 * Boots a REAL PostgreSQL in a throwaway Docker container, runs the REAL Flyway
 * migrations against it, and boots the FULL application context (security chain,
 * services, repositories). The closest thing to production we can run in CI, and it
 * needs no local Postgres — Testcontainers manages the database lifecycle.
 *
 * SINGLETON-CONTAINER pattern: the container is started ONCE in a static initializer
 * and is deliberately NEVER stopped per test class. We do NOT use @Testcontainers /
 * @Container here, because Spring caches and REUSES the application context across all
 * integration test classes — if the per-class @Container lifecycle stopped the database
 * after the first class, the next class would reuse a context wired to a dead container
 * (and every query would hang until the connection pool times out). Leaving the container
 * running for the whole JVM keeps the cached context valid; the Ryuk reaper removes the
 * container when the JVM exits.
 *
 * @ServiceConnection wires the container's JDBC url/user/password straight into Spring's
 * datasource, so we never touch application.properties (which points at host port 5433).
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    // Redis for the cache (Phase 7). @ServiceConnection(name = "redis") lets Spring Boot wire
    // spring.data.redis.host/port from this GenericContainer's mapped port automatically.
    @ServiceConnection(name = "redis")
    static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }
}
