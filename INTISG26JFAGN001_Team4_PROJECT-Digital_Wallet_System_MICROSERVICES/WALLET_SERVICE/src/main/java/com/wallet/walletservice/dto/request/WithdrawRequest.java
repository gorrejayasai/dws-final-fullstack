package com.wallet.walletservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Schema(description = "Withdraw funds from a wallet")
public record WithdrawRequest(

        @NotNull(message = "amount is required")
        @Positive(message = "amount must be positive")
        @Schema(example = "100.00")
        BigDecimal amount,

        @NotBlank(message = "currency is required")
        @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO 4217 code")
        @Schema(description = "Only INR is currently supported", example = "INR",
                defaultValue = "INR")
        String currency
) {}