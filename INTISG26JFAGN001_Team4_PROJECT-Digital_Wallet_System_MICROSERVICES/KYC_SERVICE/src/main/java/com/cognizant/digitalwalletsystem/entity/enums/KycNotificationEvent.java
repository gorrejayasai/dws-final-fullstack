package com.cognizant.digitalwalletsystem.entity.enums;

/**
 * Enumeration of KYC-related notification events.
 *
 * These template codes map to notification templates in the Notification Service.
 * Each event represents a significant step in the KYC lifecycle that requires notification.
 *
 * Template matching:
 *   KYC_SUBMITTED   → "kyc_submission_template"       (triggered by user)
 *   KYC_UPDATED     → "kyc_update_template"           (triggered by user updating their KYC)
 *   KYC_APPROVED    → "kyc_approved_template"         (triggered by admin approval)
 *   KYC_REJECTED    → "kyc_rejection_template"        (triggered by admin rejection)
 *
 * Note: These template codes must exist in the Notification Service's TemplateRegistry
 * for the notifications to be sent successfully.
 */
public enum KycNotificationEvent {
    // User submits their KYC for the first time
    KYC_SUBMITTED("kyc_submitted"),

    // User updates their submitted/rejected KYC documents
    KYC_UPDATED("kyc_updated"),

    // Admin approves the KYC after document verification
    KYC_APPROVED("kyc_approved"),

    // Admin rejects the KYC due to invalid/unclear documents
    KYC_REJECTED("kyc_rejected");

    // The template code that identifies the template in Notification Service
    private final String templateCode;

    KycNotificationEvent(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getTemplateCode() {
        return templateCode;
    }
}
