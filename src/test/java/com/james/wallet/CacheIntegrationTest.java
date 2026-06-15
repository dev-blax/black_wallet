package com.james.wallet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the Phase 7 caching behaviour deterministically by inspecting the CacheManager
 * directly (no reliance on timing). Runs against real Postgres + real Redis (Testcontainers).
 */
class CacheIntegrationTest extends AbstractIntegrationTest {

    @Autowired AccountService accountService;
    @Autowired AccountRepository accounts;
    @Autowired UserRepository users;
    @Autowired CacheManager cacheManager;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private Long newAccountId() {
        User user = users.save(new User("cache-" + System.nanoTime() + "@example.com",
                "cache", passwordEncoder.encode("password123")));
        return accounts.save(new Account(user, "USD")).getId();
    }

    private Cache.ValueWrapper cached(Long accountId) {
        Cache cache = cacheManager.getCache(CacheConfig.ACCOUNT_VIEWS);
        assertThat(cache).isNotNull();
        return cache.get(accountId);
    }

    @Test
    void readPopulatesCacheAndDepositEvictsIt() {
        Long accountId = newAccountId();

        // Nothing cached yet.
        assertThat(cached(accountId)).isNull();

        // First read goes to the DB and populates the cache.
        AccountResponse view = accountService.getAccountView(accountId);
        assertThat(view.balance()).isEqualByComparingTo("0");
        assertThat(cached(accountId)).isNotNull();

        // A deposit must evict the now-stale cached view.
        accountService.deposit(accountId, new BigDecimal("50.00"));
        assertThat(cached(accountId)).isNull();

        // Next read re-populates with the fresh balance.
        AccountResponse refreshed = accountService.getAccountView(accountId);
        assertThat(refreshed.balance()).isEqualByComparingTo("50.00");
        assertThat(cached(accountId)).isNotNull();
    }

    @Test
    void cachedReadDoesNotHitDatabaseAgain() {
        Long accountId = newAccountId();

        // Populate the cache.
        accountService.getAccountView(accountId);

        // Mutate the DB directly, bypassing the service (so no eviction happens).
        Account account = accounts.findById(accountId).orElseThrow();
        account.credit(new BigDecimal("999.00"));
        accounts.saveAndFlush(account);

        // The cached read still returns the OLD balance — proving it served from cache, not the DB.
        assertThat(accountService.getAccountView(accountId).balance()).isEqualByComparingTo("0");
    }
}
