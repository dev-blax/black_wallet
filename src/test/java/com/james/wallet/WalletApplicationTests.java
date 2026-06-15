package com.james.wallet;

import org.junit.jupiter.api.Test;

/**
 * Smoke test: the full application context boots successfully.
 *
 * Extends AbstractIntegrationTest so it boots against a Testcontainers Postgres
 * instead of requiring a local database on host port 5433.
 */
class WalletApplicationTests extends AbstractIntegrationTest {

	@Test
	void contextLoads() {
	}

}
