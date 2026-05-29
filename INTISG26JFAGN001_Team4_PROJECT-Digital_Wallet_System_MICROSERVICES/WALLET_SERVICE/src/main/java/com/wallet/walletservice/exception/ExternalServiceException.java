package com.wallet.walletservice.exception;

import lombok.Getter;

@Getter
public class ExternalServiceException extends RuntimeException {
    private final String serviceName;
    public ExternalServiceException(String serviceName, String msg) {
        super(serviceName + " is unavailable: " + msg);
        this.serviceName = serviceName;
    }
}