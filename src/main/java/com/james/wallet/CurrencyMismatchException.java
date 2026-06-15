package com.james.wallet;

public class CurrencyMismatchException extends RuntimeException {
    public CurrencyMismatchException(String source, String destination) {
        super("Currency mismatch: source=" + source + ", destination=" + destination);
    }
}
