package com.cognizant.digitalwalletsystem.exception;

public class DeniedAccessException extends RuntimeException {
    public DeniedAccessException(String message) {
        super(message);
    }
}
