package com.wallet.walletservice.exception.wallet;

import com.wallet.walletservice.exception.common.BusinessRuleException;

public class WalletClosedException extends BusinessRuleException {
    public WalletClosedException(Long id) {
        super("WALLET_CLOSED", "Wallet " + id + " is permanently closed. No operations allowed.");
    }

    public WalletClosedException(String username) {
        super("WALLET_CLOSED", "Wallet of @" + username + " is permanently closed. No operations allowed.");
    }
}