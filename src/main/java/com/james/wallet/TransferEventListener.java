package com.james.wallet;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reacts to a completed transfer (Phase 7) — e.g. send a notification or write an audit trail.
 * Also records business metrics (Phase 8).
 *
 * Two annotations carry the important semantics:
 *
 *  - @TransactionalEventListener(phase = AFTER_COMMIT): the handler fires ONLY after the
 *    transfer's database transaction actually commits. If the transaction rolls back, no
 *    notification is sent — we never tell a user "money moved" when it didn't. A plain
 *    @EventListener would fire immediately on publish, before commit, which would be a bug.
 *
 *  - @Async: the handler runs on the eventTaskExecutor thread pool, NOT the request thread,
 *    so a slow notification never delays the HTTP response to the caller.
 *
 * Recording the metrics here (after commit) is deliberate: the counter therefore reflects only
 * transfers that truly succeeded, never ones that rolled back.
 */
@Component
public class TransferEventListener {

    private static final Logger log = LoggerFactory.getLogger(TransferEventListener.class);

    private final Counter completedCounter;
    private final DistributionSummary amountSummary;

    public TransferEventListener(MeterRegistry meterRegistry) {
        // Counter: how many transfers have completed (rate, totals).
        this.completedCounter = Counter.builder("wallet.transfers.completed")
                .description("Number of successfully committed transfers")
                .register(meterRegistry);
        // DistributionSummary: the distribution of transfer amounts (count, sum, max, histogram).
        this.amountSummary = DistributionSummary.builder("wallet.transfers.amount")
                .description("Distribution of transfer amounts")
                .baseUnit("currency-minor")
                .register(meterRegistry);
    }

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransferCompleted(TransferCompletedEvent event) {
        completedCounter.increment();
        amountSummary.record(event.amount().doubleValue());

        // Stand-in for a real side effect (push notification, email, audit log).
        log.info("Transfer {} completed: {} from account {} to account {} [thread={}]",
                event.transferId(), event.amount(),
                event.sourceAccountId(), event.destinationAccountId(),
                Thread.currentThread().getName());
    }
}
