package com.james.wallet;

public class SameAccountTransferException extends RuntimeException {
    public SameAccountTransferException(Long accountId) {
        super("Source and destination cannot be the same account: " + accountId);
    }
}
