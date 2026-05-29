package com.cognizant.digitalwalletsystem.exception;

public class KycAlreadyExistsException extends RuntimeException {

    public KycAlreadyExistsException(Long userId) {
        super("A KYC request already exists for userId: " + userId);
    }
}
