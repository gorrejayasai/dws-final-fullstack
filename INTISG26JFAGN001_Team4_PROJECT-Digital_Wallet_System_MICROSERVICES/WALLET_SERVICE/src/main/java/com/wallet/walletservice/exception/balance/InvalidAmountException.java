package com.wallet.walletservice.exception.balance;

import com.wallet.walletservice.exception.common.BusinessRuleException;

import java.math.BigDecimal;

public class InvalidAmountException extends BusinessRuleException {
    public InvalidAmountException(BigDecimal amount, String reason) {
        super("INVALID_AMOUNT", "Invalid amount: " + amount + ". " + reason);
    }
}