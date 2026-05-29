package com.wallet.walletservice.exception.currency;

import com.wallet.walletservice.exception.common.BusinessRuleException;

public class UnsupportedCurrencyException extends BusinessRuleException {
    public UnsupportedCurrencyException(String currency) {
        super("UNSUPPORTED_CURRENCY",
                "Currency '" + currency + "' is not supported. " +
                        "Only INR is currently supported.");
    }
}