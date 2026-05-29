package com.wallet.notification.dto;

import com.wallet.notification.enums.NotificationChannel;
import com.wallet.notification.enums.NotificationStatus;
import java.time.Instant;

public record NotificationResponse(
        Long id,
        Long userId,
        NotificationChannel channel,
        NotificationStatus status,
        String templateCode,
        String recipient,
        int attempts,
        String lastError,
        Instant createdAt,
        Instant sentAt
) {}