package com.james.wallet;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransferResponse(
        UUID id,
        Long sourceAccountId,
        Long destinationAccountId,
        BigDecimal amount,
        String idempotencyKey,
        OffsetDateTime createdAt
) {
    public static TransferResponse from(Transfer t) {
        return new TransferResponse(
                t.getId(),
                t.getSourceAccountId(),
                t.getDestinationAccountId(),
                t.getAmount(),
                t.getIdempotencyKey(),
                t.getCreatedAt()
        );
    }
}
