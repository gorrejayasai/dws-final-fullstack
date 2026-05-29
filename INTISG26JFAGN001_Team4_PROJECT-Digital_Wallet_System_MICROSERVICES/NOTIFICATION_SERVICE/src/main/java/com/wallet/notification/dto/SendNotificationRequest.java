package com.wallet.notification.dto;

import com.wallet.notification.enums.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendNotificationRequest(
        @NotNull Long userId,
        @NotNull NotificationChannel channel,
        @NotBlank String templateCode,
        @NotBlank String payload
) {}