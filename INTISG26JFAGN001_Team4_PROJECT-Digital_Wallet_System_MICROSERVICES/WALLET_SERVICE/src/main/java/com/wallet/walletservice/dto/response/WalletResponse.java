package com.wallet.walletservice.dto.response;

import com.wallet.walletservice.enums.WalletStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record WalletResponse(
        Long id, Long userId, String currency,
        BigDecimal availableBalance, BigDecimal heldBalance,
        WalletStatus status, Instant createdAt
) {}