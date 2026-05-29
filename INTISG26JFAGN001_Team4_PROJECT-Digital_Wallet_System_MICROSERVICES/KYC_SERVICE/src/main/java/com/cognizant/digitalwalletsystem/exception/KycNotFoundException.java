package com.cognizant.digitalwalletsystem.exception;

public class KycNotFoundException extends RuntimeException {

    public KycNotFoundException(String message) {
        super(message);
    }

    public KycNotFoundException(Long userId) {
        super("KYC request not found for userId: " + userId);
    }
}
