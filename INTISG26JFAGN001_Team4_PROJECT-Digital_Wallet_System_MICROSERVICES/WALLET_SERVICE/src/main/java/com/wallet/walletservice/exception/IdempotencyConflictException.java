package com.wallet.walletservice.exception;

//For Handling duplicate requests from the same user
public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String key) {
        super("Request with idempotency key '" + key + "' was already processed.");
    }
}

