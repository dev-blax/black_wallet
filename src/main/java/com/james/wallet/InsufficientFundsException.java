package com.james.wallet;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {

    private final Long accountId;
    private final BigDecimal available;
    private final BigDecimal requested;

    public InsufficientFundsException(Long accountId, BigDecimal available, BigDecimal requested) {
        super("Insufficient funds on account " + accountId + ": available=" + available + ", requested=" + requested);
        this.accountId = accountId;
        this.available = available;
        this.requested = requested;
    }

    public Long getAccountId() { return accountId; }
    public BigDecimal getAvailable() { return available; }
    public BigDecimal getRequested() { return requested; }
}
