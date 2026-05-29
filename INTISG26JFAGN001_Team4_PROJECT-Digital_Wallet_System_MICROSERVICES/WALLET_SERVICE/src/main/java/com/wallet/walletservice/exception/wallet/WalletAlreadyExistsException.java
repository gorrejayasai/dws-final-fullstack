package com.wallet.walletservice.exception.wallet;

import com.wallet.walletservice.exception.common.BusinessRuleException;

public class WalletAlreadyExistsException extends BusinessRuleException {
    public WalletAlreadyExistsException(Long userId) {
        super("WALLET_ALREADY_EXISTS",
                "A wallet already exists for user " + userId
                        + ". Each user can have only one wallet.");
    }
}