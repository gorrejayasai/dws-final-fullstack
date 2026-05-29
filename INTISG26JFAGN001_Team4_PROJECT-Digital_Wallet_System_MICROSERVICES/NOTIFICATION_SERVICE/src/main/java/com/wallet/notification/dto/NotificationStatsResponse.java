package com.wallet.notification.dto;

public record NotificationStatsResponse(
        long totalPending,
        long totalSent,
        long totalFailed
) {}
