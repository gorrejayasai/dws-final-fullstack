package com.wallet.walletservice.exception.currency;

import com.wallet.walletservice.exception.common.BusinessRuleException;

public class CurrencyMismatchException extends BusinessRuleException {
    public CurrencyMismatchException(String walletCurrency, String requestCurrency) {
        super("CURRENCY_MISMATCH",
                "Currency mismatch. Wallet currency: " + walletCurrency
                        + ", Request currency: " + requestCurrency + ".");
    }
}