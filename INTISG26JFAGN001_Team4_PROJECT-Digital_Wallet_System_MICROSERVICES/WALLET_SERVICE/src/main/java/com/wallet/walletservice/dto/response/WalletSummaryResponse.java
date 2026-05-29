package com.wallet.walletservice.dto.response;

public record WalletSummaryResponse(
        long total,
        long active,
        long frozen,
        long closed
) {}
