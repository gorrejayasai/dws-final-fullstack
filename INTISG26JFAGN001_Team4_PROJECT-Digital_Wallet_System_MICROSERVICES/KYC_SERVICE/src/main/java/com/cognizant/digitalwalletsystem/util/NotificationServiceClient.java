package com.cognizant.digitalwalletsystem.util;

import com.cognizant.digitalwalletsystem.dto.SendNotificationRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * OpenFeign declarative HTTP client for Notification Service.
 * 
 * Handles sending notifications to users for various KYC events:
 *   - KYC Submission
 *   - KYC Update
 *   - KYC Approval
 *   - KYC Rejection
 *
 * Contract expected from Notification Service:
 *   POST /notify/send         →  Creates & sends notification synchronously
 *   POST /notify/send-async   →  Queues notification for asynchronous processing
 *
 * Configuration:
 *   The base URL is configured via property: app.notification-service.base-url
 *   Set in application-dev.yml or application.yml
 *
 * ──────────────────────────────────────────────────────────────────────────
 * Integration note:
 *   Standalone  : url = http://localhost:8085 (set in application-dev.yml)
 *   With Eureka : Remove the url attribute and rely on service discovery —
 *                Feign will resolve "notification-service" via Eureka.
 *
 */
@FeignClient(
    name = "notification-service"
)
public interface NotificationServiceClient {

    /**
     * Send a notification synchronously (blocking).
     * 
     * Use this only if you need confirmation that the notification was created
     * and sent before proceeding. This will block the KYC operation.
     *
     * @param request containing userId, channel, templateCode, and payload (JSON string)
     * @return 201 Created (notification was processed)
     */
    @PostMapping("/notify/send")
    ResponseEntity<Object> send(@RequestBody SendNotificationRequest request);

    /**
     * Send a notification asynchronously (non-blocking).
     *
     * Request is queued for background processing. Returns immediately without waiting
     * for the notification to be sent. Ideal for non-critical notifications that
     * shouldn't block business operations like KYC submission, approval, or rejection.
     *
     * @param request containing userId, channel, templateCode, and payload (JSON string)
     * @return 202 Accepted (notification was queued for processing)
     */
    @PostMapping("/notify/send-async")
    ResponseEntity<Object> sendAsync(@RequestBody SendNotificationRequest request);
}


