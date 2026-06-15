package com.james.wallet;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AccountService {

    private final AccountRepository accounts;
    private final LedgerEntryRepository ledger;

    public AccountService(AccountRepository accounts, LedgerEntryRepository ledger) {
        this.accounts = accounts;
        this.ledger = ledger;
    }

    /**
     * Faucet deposit. NOT idempotent — demo only. In real life a deposit
     * would carry an idempotency key (e.g. the Stripe payment intent id).
     *
     * @CacheEvict: a deposit changes the balance, so the cached AccountResponse for this
     * account is now stale and MUST be removed. Without this, GET /accounts/{id} would keep
     * serving the old balance until the 10-minute TTL expired. This is the golden rule of
     * caching mutable data: every write path evicts what it changes.
     */
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.ACCOUNT_VIEWS, key = "#accountId")
    public Account deposit(Long accountId, BigDecimal amount) {
        Account account = accounts.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        account.credit(amount);
        ledger.save(LedgerEntry.deposit(accountId, amount));
        return account;
    }

    @Transactional(readOnly = true)
    public Account findById(Long accountId) {
        return accounts.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    /**
     * Cached read of an account as a DTO, used by GET /accounts/{id}.
     *
     * @Cacheable: the first call hits the database and stores the AccountResponse in the
     * "accountViews" cache under the account id; subsequent calls return the cached value
     * without touching Postgres, until the entry is evicted (on deposit/transfer) or its TTL
     * lapses. We return a DTO rather than the JPA entity so there is no lazy association to
     * serialize into Redis.
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.ACCOUNT_VIEWS, key = "#accountId")
    public AccountResponse getAccountView(Long accountId) {
        return AccountResponse.from(findById(accountId));
    }
}
