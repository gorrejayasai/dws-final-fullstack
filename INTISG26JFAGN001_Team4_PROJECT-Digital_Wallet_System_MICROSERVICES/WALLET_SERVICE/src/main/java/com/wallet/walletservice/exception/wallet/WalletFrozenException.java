package com.wallet.walletservice.exception.wallet;

import com.wallet.walletservice.exception.common.BusinessRuleException;

public class WalletFrozenException extends BusinessRuleException {
    public WalletFrozenException(Long id) {
        super("WALLET_FROZEN", "Wallet " + id + " is frozen. Contact support to unfreeze.");
    }

    public WalletFrozenException(String username) {
        super("WALLET_FROZEN", "Wallet of @" + username + " is frozen. Contact support to unfreeze.");
    }
}