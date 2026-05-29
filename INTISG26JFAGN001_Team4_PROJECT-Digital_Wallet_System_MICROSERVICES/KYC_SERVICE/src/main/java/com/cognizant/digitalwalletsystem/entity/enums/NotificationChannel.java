package com.cognizant.digitalwalletsystem.entity.enums;

/**
 * Enumeration of supported notification channels/delivery mechanisms.
 * 
 * Each channel represents a different communication method:
 * - EMAIL: Email notifications
 * - SMS: Short Message Service (SMS)
 * - PUSH_NOTIFICATION: Mobile/Web push notifications
 */
public enum NotificationChannel {
    EMAIL,                // Email-based notification
    SMS,                  // SMS-based notification  
    PUSH_NOTIFICATION     // Push notification (mobile/web)
}

