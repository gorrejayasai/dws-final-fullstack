package com.wallet.walletservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Create the wallet for a user (one per user)")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateWalletRequest{

        @NotNull(message = "userId is required")
        @Schema(example = "1001")
        private Long userId;

        @NotBlank(message = "currency is required")
        @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO 4217 code")
        @Schema(
                description = "Only INR is currently supported",
                example = "INR"
        )
        private String currency;
}