package com.cognizant.TransactionService.dto;

import com.cognizant.TransactionService.entity.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body to create a new financial transaction")
public class CreateTransactionRequest {

    @NotNull(message = "Wallet Id is required")
    @Schema(description = "ID of the source wallet", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long walletId;

    @Schema(description = "username of the source wallet", example = "Dinesh", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "ID of the destination wallet — required only for TRANSFER type", example = "1002")
    private Long targetWalletId;

    @Schema(description = "userId of the destination wallet owner — required only for TRANSFER type", example = "7")
    private Long targetUserId;

    @Schema(description = "username of the target wallet", example = "Mounya", requiredMode = Schema.RequiredMode.REQUIRED)
    private String targetUsername;

    @NotNull(message = "Transaction type is required")
    @Schema(description = "Type of transaction", example = "TOPUP", allowableValues = {"TOPUP", "WITHDRAW", "TRANSFER"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    private TransactionType type;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 14, fraction = 4, message = "Invalid amount precision")
    @Schema(description = "Transaction amount (min 0.01, max 14 integer digits, 4 decimal places)", example = "500.00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @Size(max = 10, message = "Currency code is too long")
    @Schema(description = "ISO 4217 currency code. Defaults to INR if not provided", example = "INR")
    private String currency;

    @NotNull(message = "Idempotency Key is required")
    @Size(max = 128)
    @Schema(description = "Unique client-generated key to guarantee idempotency (max 128 chars)",
            example = "wallet-1001-topup-uuid-abc123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String idempotencyKey;

    @Schema(description = "Optional JSON metadata (e.g. gateway reference, device info, remarks)", example = "{\"note\": \"rent\"}")
    private String metadata;
}
