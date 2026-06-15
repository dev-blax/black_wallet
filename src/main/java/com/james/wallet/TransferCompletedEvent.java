package com.james.wallet;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Domain event published when a transfer is successfully persisted (Phase 7).
 *
 * Carries only plain, immutable data — no JPA entities — so listeners running after the
 * transaction (and on another thread) never touch detached/lazy entity state.
 */
public record TransferCompletedEvent(
        UUID transferId,
        Long sourceAccountId,
        Long destinationAccountId,
        BigDecimal amount
) {
    public static TransferCompletedEvent from(Transfer transfer) {
        return new TransferCompletedEvent(
                transfer.getId(),
                transfer.getSourceAccountId(),
                transfer.getDestinationAccountId(),
                transfer.getAmount());
    }
}
