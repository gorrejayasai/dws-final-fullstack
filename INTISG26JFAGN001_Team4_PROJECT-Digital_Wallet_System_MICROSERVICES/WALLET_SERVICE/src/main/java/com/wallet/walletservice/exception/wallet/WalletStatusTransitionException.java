package com.wallet.walletservice.exception.wallet;

import com.wallet.walletservice.enums.WalletStatus;
import com.wallet.walletservice.exception.common.BusinessRuleException;

public class WalletStatusTransitionException extends BusinessRuleException {
    public WalletStatusTransitionException(Long walletId, WalletStatus from, WalletStatus to) {
        super("INVALID_STATUS_TRANSITION",
                "Wallet " + walletId + " cannot transition from " + from + " to " + to + ".");
    }
}