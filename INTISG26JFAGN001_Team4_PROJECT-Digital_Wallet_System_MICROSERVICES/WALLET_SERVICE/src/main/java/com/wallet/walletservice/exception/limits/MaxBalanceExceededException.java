package com.wallet.walletservice.exception.limits;

import com.wallet.walletservice.exception.common.BusinessRuleException;

import java.math.BigDecimal;

public class MaxBalanceExceededException extends BusinessRuleException {
    public MaxBalanceExceededException(Long id, BigDecimal max, BigDecimal resulting) {
        super("MAX_BALANCE_EXCEEDED",
                "Wallet " + id + " would exceed max balance. Max: " + max + ", Resulting: " + resulting + ".");
    }
}

