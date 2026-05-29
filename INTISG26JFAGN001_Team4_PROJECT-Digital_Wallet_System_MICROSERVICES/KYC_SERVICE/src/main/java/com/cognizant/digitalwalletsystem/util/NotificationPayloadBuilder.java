package com.cognizant.digitalwalletsystem.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for building JSON payloads to be sent to the Notification Service.
 *
 * This class encapsulates the logic for constructing notification payloads that contain
 * template variables and context information needed by notification templates.
 *
 * Each notification requires:
 *   1. userId - who should receive this notification
 *   2. event type - which template to use
 *   3. payload - JSON string with template variables
 *
 * Usage example:
 *   String payload = NotificationPayloadBuilder.buildKycSubmittedPayload(123L);
 *   String payload = NotificationPayloadBuilder.buildKycApprovedPayload(456L, "Approved");
 */
@Slf4j
public class NotificationPayloadBuilder {

    private static final ObjectMapper objectMapper = new ObjectMapper();


    /**
     * Builds payload for KYC_SUBMITTED event
     * Triggered when a user submits their KYC for the first time
     *
     * @param userId the ID of the user who submitted KYC
     * @return JSON string containing submission details
     */
    public static String buildKycSubmittedPayload(Long userId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("event", "KYC_SUBMITTED");
        payload.put("message", "Your KYC has been submitted successfully and is pending review");
        return mapToJsonString(payload);
    }




    /**
     * Alternative overload: Build KYC submission payload with user name
     *
     * @param userName the name of the user who submitted KYC
     * @return JSON string containing submission details
     */
    public static String buildKycSubmittedPayload(String userName) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userName", userName);
        payload.put("event", "KYC_SUBMITTED");
        payload.put("message", "Your KYC has been submitted successfully and is pending review");
        return mapToJsonString(payload);
    }

    /**
     * Builds payload for KYC_UPDATED event
     * Triggered when a user updates their KYC documents (PENDING or REJECTED state)
     *
     * @param userId the ID of the user who updated KYC
     * @param previousStatus the status before the update (PENDING or REJECTED)
     * @return JSON string containing update details
     */
    public static String buildKycUpdatedPayload(Long userId, String previousStatus) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("event", "KYC_UPDATED");
        payload.put("previousStatus", previousStatus);
        payload.put("message", "Your KYC has been updated and is pending re-review");
        return mapToJsonString(payload);
    }

    /**
     * Alternative overload: Build KYC update payload with user name
     *
     * @param userName the name of the user who updated KYC
     * @param previousStatus the status before the update
     * @return JSON string containing update details
     */
    public static String buildKycUpdatedPayload(String userName, String previousStatus) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userName", userName);
        payload.put("event", "KYC_UPDATED");
        payload.put("previousStatus", previousStatus);
        payload.put("message", "Your KYC has been updated and is pending re-review");
        return mapToJsonString(payload);
    }

    /**
     * Build payload for KYC_APPROVED event
     * Triggered when admin approves a user's KYC
     *
     * @param userId the ID of the approved user
     * @param remarks optional remarks from admin
     * @return JSON string containing approval details
     */
    public static String buildKycApprovedPayload(Long userId, String remarks) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("event", "KYC_APPROVED");
        payload.put("status", "APPROVED");
        payload.put("remarks", remarks != null ? remarks : "KYC approved successfully");
        payload.put("message", "Congratulations! Your KYC has been approved. Your wallet is now fully accessible.");
        return mapToJsonString(payload);
    }

    /**
     * Alternative overload: Build KYC approval payload with user name
     *
     * @param userName the name of the approved user
     * @param remarks optional remarks from admin
     * @return JSON string containing approval details
     */
    public static String buildKycApprovedPayload(String userName, String remarks) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userName", userName);
        payload.put("event", "KYC_APPROVED");
        payload.put("status", "APPROVED");
        payload.put("remarks", remarks != null ? remarks : "KYC approved successfully");
        payload.put("message", "Congratulations! Your KYC has been approved. Your wallet is now fully accessible.");
        return mapToJsonString(payload);
    }

    /**
     * Build payload for KYC_REJECTED event
     * Triggered when admin rejects a user's KYC
     *
     * @param userId the ID of the user whose KYC was rejected
     * @param remarks mandatory reason for rejection (required by admin)
     * @return JSON string containing rejection details
     */
    public static String buildKycRejectedPayload(Long userId, String remarks) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("event", "KYC_REJECTED");
        payload.put("status", "REJECTED");
        payload.put("remarks", remarks);
        payload.put("message", "Your KYC has been rejected. Please review the remarks and resubmit your documents.");
        return mapToJsonString(payload);
    }

    /**
     * Alternative overload: Build KYC rejection payload with user name
     *
     * @param userName the name of the user whose KYC was rejected
     * @param remarks mandatory reason for rejection
     * @return JSON string containing rejection details
     */
    public static String buildKycRejectedPayload(String userName, String remarks) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userName", userName);
        payload.put("event", "KYC_REJECTED");
        payload.put("status", "REJECTED");
        payload.put("remarks", remarks);
        payload.put("message", "Your KYC has been rejected. Please review the remarks and resubmit your documents.");
        return mapToJsonString(payload);
    }

    /**
     * Generic helper method to convert a Map to JSON string
     *
     * @param map the map to convert
     * @return JSON string representation
     */
    private static String mapToJsonString(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.error("Failed to serialize notification payload: {}", e.getMessage());
            // If serialization fails, return empty JSON object as fallback
            return "{}";
        }
    }
}