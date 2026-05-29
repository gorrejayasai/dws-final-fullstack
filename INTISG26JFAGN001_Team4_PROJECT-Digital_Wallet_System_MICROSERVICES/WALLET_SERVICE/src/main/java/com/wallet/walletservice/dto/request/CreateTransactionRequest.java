package com.wallet.walletservice.dto.request;

import com.wallet.walletservice.enums.TransactionType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTransactionRequest {

    private Long walletId;
    private String username;

    // Required only for transaction of type TRANSFER
    private Long targetWalletId;
    private Long targetUserId;
    private String targetUsername;

    private TransactionType type;
    private BigDecimal amount;
    private String currency;
    private String idempotencyKey;
    private String metadata;
}
