package com.wallet.walletservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Schema(description = "Transfer funds to another wallet (resolved via recipient username)")
public record TransferRequest(

        @NotNull(message = "amount is required")
        @Positive(message = "amount must be positive")
        @Schema(example = "200.00")
        BigDecimal amount,

        @NotBlank(message = "currency is required")
        @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO 4217 code")
        @Schema(description = "Only INR is currently supported", example = "INR",
                defaultValue = "INR")
        String currency,

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "username must be between 3 and 50 characters")
        @Schema(description = "Username of the sender", example = "john_doe")
        String username,

        @NotBlank(message = "targetUsername is required")
        @Size(min = 3, max = 50, message = "targetUsername must be between 3 and 50 characters")
        @Schema(description = "Username of the recipient", example = "john_doe")
        String targetUsername
) {}