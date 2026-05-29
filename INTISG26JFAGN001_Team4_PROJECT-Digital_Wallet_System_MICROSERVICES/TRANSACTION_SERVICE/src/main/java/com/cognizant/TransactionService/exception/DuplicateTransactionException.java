package com.cognizant.TransactionService.exception;

import lombok.Getter;

@Getter
public class DuplicateTransactionException extends RuntimeException {

    private final String existingTransactionId;

    public DuplicateTransactionException(String existingTransactionId){
        super("Duplicate idempotency key - existing transaction found " + existingTransactionId);
        this.existingTransactionId = existingTransactionId;
    }
}
