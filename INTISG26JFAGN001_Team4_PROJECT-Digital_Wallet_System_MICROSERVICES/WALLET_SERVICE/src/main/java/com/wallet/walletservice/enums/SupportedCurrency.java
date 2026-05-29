package com.wallet.walletservice.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum SupportedCurrency {

    INR(
            new BigDecimal("1000000.0000"),
            new BigDecimal("50000.0000"),
            new BigDecimal("100000.0000"),
            new BigDecimal("50000.0000")
    );

    private final BigDecimal maxBalance;
    private final BigDecimal maxTopupAmount;
    private final BigDecimal maxTransferAmount;
    private final BigDecimal maxWithdrawAmount;

    public static SupportedCurrency fromCode(String code) {
        try {
            return SupportedCurrency.valueOf(code.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(code);
        }
    }
}