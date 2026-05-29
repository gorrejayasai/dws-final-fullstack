package com.cognizant.digitalwalletsystem.dto;

import com.cognizant.digitalwalletsystem.entity.enums.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Request DTO for sending notifications to the Notification Service.
//Used by KYC Service to trigger notifications for various KYC events.
//The channel and templateCode determine how the notification is sent and what template is used.

public record SendNotificationRequest(
        // The userId who should receive the notification
        @NotNull(message = "userId cannot be null")
        Long userId,

        // Delivery channel: EMAIL, SMS, or PUSH_NOTIFICATION
        @NotNull(message = "Notification channel cannot be null")
        NotificationChannel channel,

        // Template code that determines the message template in Notification Service
        // Examples: KYC_SUBMITTED, KYC_APPROVED, KYC_REJECTED, KYC_UPDATED
        @NotBlank(message = "Template code cannot be blank")
        String templateCode,

        // JSON string containing template variables and message context
        // Example: {"userName":"John Doe", "kycStatus":"PENDING", "remarks":"Documents are clear"}
        @NotBlank(message = "Payload cannot be blank")
        String payload
) {}

