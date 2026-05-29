package com.wallet.walletservice.exception.balance;

import com.wallet.walletservice.exception.common.BusinessRuleException;

import java.math.BigDecimal;

public class InsufficientBalanceException extends BusinessRuleException {
    public InsufficientBalanceException(Long walletId, BigDecimal available, BigDecimal requested) {
        super("WALLET_INSUFFICIENT_FUNDS",
                "Wallet " + walletId + " has insufficient balance. Available: "
                        + available + ", Requested: " + requested + ".");
    }
}