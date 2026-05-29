package com.wallet.walletservice.exception.transfer;

import com.wallet.walletservice.exception.common.BusinessRuleException;

public class SelfTransferException extends BusinessRuleException {
    public SelfTransferException(Long id) {
        super("WALLET_SELF_TRANSFER", "Cannot transfer funds to the same wallet: " + id + ".");
    }
}