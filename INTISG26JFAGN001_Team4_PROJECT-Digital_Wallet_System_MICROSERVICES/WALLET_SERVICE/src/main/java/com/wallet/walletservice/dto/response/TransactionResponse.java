
package com.wallet.walletservice.dto.response;

import com.wallet.walletservice.enums.TransactionStatus;
import com.wallet.walletservice.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private Long id;
    private String transactionId;
    private String idempotencyKey;
    private Long walletId;
    private Long targetWalletId;
    private Long userId;
    private Long targetUserId;
    private String username;
    private String targetUsername;
    private TransactionType type;
    private BigDecimal amount;
    private String currency;
    private TransactionStatus status;
    private String metadata;
    private Instant createdAt;
    private Instant updatedAt;
}