package com.cognizant.TransactionService.dto;

import com.cognizant.TransactionService.entity.enums.TransactionStatus;
import com.cognizant.TransactionService.entity.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Represents a recorded financial transaction")
public class TransactionResponse {

    @Schema(description = "Internal database ID", example = "42")
    private Long id;

    @Schema(description = "Business-level transaction identifier", example = "txn-3f8a9c2e4d1b")
    private String transactionId;

    @Schema(description = "Source wallet ID", example = "1001")
    private Long walletId;

    @Schema(description = "Destination wallet ID — only set for TRANSFER transactions", example = "1002")
    private Long targetWalletId;

    @Schema(description = "ID of the user who owns this transaction", example = "7")
    private Long userId;

    @Schema(description = "ID of the user who received this transaction", example = "4")
    private Long targetUserId;

    @Schema(description = "Username of the user who owns this transaction", example = "Dinesh")
    private String username;

    @Schema(description = "Username of the user who received this transaction", example = "Mounya")
    private String targetUsername;

    @Schema(description = "Type of transaction", example = "TOPUP")
    private TransactionType type;

    @Schema(description = "Transaction amount", example = "500.00")
    private BigDecimal amount;

    @Schema(description = "Currency code", example = "INR")
    private String currency;

    @Schema(description = "Current status of the transaction", example = "COMPLETED")
    private TransactionStatus status;

    @Schema(description = "Optional JSON metadata attached at creation time", example = "{\"note\": \"rent\"}")
    private String metadata;

    @Schema(description = "Timestamp when the transaction was created (UTC)")
    private Instant createdAt;

    @Schema(description = "Idempotency key supplied by the caller", example = "wallet-1001-topup-uuid-abc123")
    private String idempotencyKey;

    @Schema(description = "Timestamp when the transaction was last updated (UTC)")
    private Instant updatedAt;
}
