package com.wallet.notification.template;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class TemplateRegistry {

    private static final Map<String, String> SUBJECTS = Map.ofEntries(
            Map.entry("TOPUP_SUCCESS",    "Your wallet top-up was successful"),
            Map.entry("TOPUP_FAILED",     "Your wallet top-up failed"),
            Map.entry("WITHDRAW_SUCCESS", "Withdrawal processed successfully"),
            Map.entry("WITHDRAW_FAILED",  "Withdrawal could not be processed"),
            Map.entry("TRANSFER_SUCCESS", "Transfer completed"),
            Map.entry("TRANSFER_FAILED",  "Transfer failed"),
            Map.entry("KYC_SUBMITTED",      "KYC Submitted for Verification"),
            Map.entry("KYC_UPDATED",      "KYC is updated and is under verification"),
            Map.entry("KYC_APPROVED",     "KYC verification approved"),
            Map.entry("KYC_REJECTED",     "KYC verification rejected"),
            Map.entry("LOW_BALANCE",      "Low balance alert")
    );

    private static final Map<String, String> TEMPLATES = Map.ofEntries(
            Map.entry("TOPUP_SUCCESS",    "topup-success"),
            Map.entry("TOPUP_FAILED",     "topup-failed"),
            Map.entry("WITHDRAW_SUCCESS", "withdraw-success"),
            Map.entry("WITHDRAW_FAILED",  "withdraw-failed"),
            Map.entry("TRANSFER_SUCCESS", "transfer-success"),
            Map.entry("TRANSFER_FAILED",  "transfer-failed"),
            Map.entry("KYC_SUBMITTED",      "kyc-submitted"),
            Map.entry("KYC_UPDATED",      "kyc-updated"),
            Map.entry("KYC_APPROVED",     "kyc-approved"),
            Map.entry("KYC_REJECTED",     "kyc-rejected"),
            Map.entry("LOW_BALANCE",      "low-balance")
    );

    public String getSubject(String code) {
        return SUBJECTS.getOrDefault(code, "Digital Wallet Notification");
    }

    public String getTemplateName(String code) {
        return TEMPLATES.getOrDefault(code, "generic");
    }
}
