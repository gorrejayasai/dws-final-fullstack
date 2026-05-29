package com.wallet.walletservice.dto.request;

import com.wallet.walletservice.enums.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendNotificationRequest(
        @NotNull Long userId,
        @NotNull NotificationChannel channel,
        @NotBlank String templateCode,
        @NotBlank String payload
) {}
