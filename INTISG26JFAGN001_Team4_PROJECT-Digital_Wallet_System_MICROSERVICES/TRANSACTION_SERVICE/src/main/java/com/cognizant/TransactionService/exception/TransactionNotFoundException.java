package com.cognizant.TransactionService.exception;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(String identifier) {
        super("Transaction not found: " + identifier);
    }
}
