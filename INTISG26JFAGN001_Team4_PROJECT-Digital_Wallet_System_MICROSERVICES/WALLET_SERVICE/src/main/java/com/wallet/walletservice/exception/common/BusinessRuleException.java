package com.wallet.walletservice.exception.common;

import lombok.Getter;

@Getter
public class BusinessRuleException extends RuntimeException {
    private final String code;
    public BusinessRuleException(String code, String msg) {
        super(msg);
        this.code = code;
    }
}