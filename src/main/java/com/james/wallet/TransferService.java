package com.james.wallet;

import io.micrometer.observation.annotation.Observed;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TransferService {

    private final AccountRepository accounts;
    private final TransferRepository transfers;
    private final LedgerEntryRepository ledger;
    private final ApplicationEventPublisher events;

    public TransferService(AccountRepository accounts,
                           TransferRepository transfers,
                           LedgerEntryRepository ledger,
                           ApplicationEventPublisher events) {
        this.accounts = accounts;
        this.transfers = transfers;
        this.ledger = ledger;
        this.events = events;
    }

    /**
     * A transfer mutates BOTH account balances, so both cached views must be evicted.
     * @Caching lets us declare two @CacheEvicts on one method. Eviction runs only after the
     * method returns normally (the default beforeInvocation=false), so a failed/rolled-back
     * transfer leaves the cache untouched. The idempotent early-return below also passes
     * through this eviction harmlessly (the views are simply re-cached on next read).
     */
    @Observed(name = "wallet.transfer",
              contextualName = "transfer",
              lowCardinalityKeyValues = {"operation", "transfer"})
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.ACCOUNT_VIEWS, key = "#sourceId"),
            @CacheEvict(cacheNames = CacheConfig.ACCOUNT_VIEWS, key = "#destinationId")
    })
    public Transfer transfer(Long sourceId, Long destinationId, BigDecimal amount, String idempotencyKey) {
        // 1. Idempotency: same key = same answer, no second movement of money.
        var existing = transfers.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        // 2. Sanity guards.
        if (sourceId.equals(destinationId)) {
            throw new SameAccountTransferException(sourceId);
        }

        // 3. Load both accounts. We deterministically order locks by ID to avoid deadlock
        //    in case two opposite transfers run concurrently (A→B and B→A).
        Long firstId = Math.min(sourceId, destinationId);
        Long secondId = Math.max(sourceId, destinationId);
        Account first  = accounts.findById(firstId).orElseThrow(() -> new AccountNotFoundException(firstId));
        Account second = accounts.findById(secondId).orElseThrow(() -> new AccountNotFoundException(secondId));

        Account source      = firstId.equals(sourceId) ? first : second;
        Account destination = firstId.equals(sourceId) ? second : first;

        // 4. Currency check.
        if (!source.getCurrency().equals(destination.getCurrency())) {
            throw new CurrencyMismatchException(source.getCurrency(), destination.getCurrency());
        }

        // 5. Move the money. debit() will throw InsufficientFundsException if balance is too low.
        source.debit(amount);
        destination.credit(amount);

        // 6. Double-entry ledger: one DEBIT, one CREDIT, both pointing at the transfer.
        Transfer transfer = transfers.save(new Transfer(sourceId, destinationId, amount, idempotencyKey));
        ledger.save(new LedgerEntry(sourceId,      LedgerEntry.EntryType.DEBIT,  amount, transfer.getId()));
        ledger.save(new LedgerEntry(destinationId, LedgerEntry.EntryType.CREDIT, amount, transfer.getId()));

        // Publish a domain event. The @TransactionalEventListener consuming it fires only
        // AFTER this transaction commits, so no notification is sent if anything below rolls back.
        events.publishEvent(TransferCompletedEvent.from(transfer));

        return transfer;
    }
}
