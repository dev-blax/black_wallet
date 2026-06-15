package com.james.wallet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves TransferService publishes a TransferCompletedEvent on a successful transfer (Phase 7).
 *
 * @RecordApplicationEvents captures every event published during the test, on the publishing
 * thread, at publish time — so this is fully deterministic and does NOT depend on the async,
 * after-commit listener having run yet. (The listener's after-commit + async semantics are a
 * separate concern, exercised by the real wiring; here we assert the event contract.)
 */
@RecordApplicationEvents
class TransferEventTest extends AbstractIntegrationTest {

    @Autowired TransferService transferService;
    @Autowired AccountRepository accounts;
    @Autowired UserRepository users;
    @Autowired ApplicationEvents events;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private Long accountWithBalance(BigDecimal balance) {
        User user = users.save(new User("evt-" + System.nanoTime() + "@example.com",
                "evt", passwordEncoder.encode("password123")));
        Account account = new Account(user, "USD");
        if (balance.signum() > 0) {
            account.credit(balance);
        }
        return accounts.save(account).getId();
    }

    @Test
    void successfulTransferPublishesEvent() {
        Long source = accountWithBalance(new BigDecimal("100.00"));
        Long destination = accountWithBalance(BigDecimal.ZERO);

        transferService.transfer(source, destination, new BigDecimal("30.00"), "evt-key-1");

        List<TransferCompletedEvent> published =
                events.stream(TransferCompletedEvent.class).toList();

        assertThat(published).hasSize(1);
        TransferCompletedEvent event = published.get(0);
        assertThat(event.sourceAccountId()).isEqualTo(source);
        assertThat(event.destinationAccountId()).isEqualTo(destination);
        assertThat(event.amount()).isEqualByComparingTo("30.00");
        assertThat(event.transferId()).isNotNull();
    }

    @Test
    void idempotentReplayDoesNotPublishASecondEvent() {
        Long source = accountWithBalance(new BigDecimal("100.00"));
        Long destination = accountWithBalance(BigDecimal.ZERO);

        // Same idempotency key twice — the second call returns the existing transfer early,
        // so no money moves again and NO second event is published.
        transferService.transfer(source, destination, new BigDecimal("10.00"), "evt-key-2");
        transferService.transfer(source, destination, new BigDecimal("10.00"), "evt-key-2");

        assertThat(events.stream(TransferCompletedEvent.class).count()).isEqualTo(1);
    }
}
