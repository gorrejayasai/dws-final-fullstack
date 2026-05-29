package com.wallet.walletservice.exception.wallet;

import com.wallet.walletservice.exception.common.ResourceNotFoundException;

public class WalletNotFoundException extends ResourceNotFoundException {
    public WalletNotFoundException(Long id) {
        super("Wallet not found with id: " + id);
    }
}