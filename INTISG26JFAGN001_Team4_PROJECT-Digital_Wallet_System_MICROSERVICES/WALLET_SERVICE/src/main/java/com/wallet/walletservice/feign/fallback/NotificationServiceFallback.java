package com.wallet.walletservice.feign.fallback;

import com.wallet.walletservice.dto.request.SendNotificationRequest;
import com.wallet.walletservice.dto.response.NotificationResponse;
import com.wallet.walletservice.exception.ExternalServiceException;
import com.wallet.walletservice.feign.NotificationServiceClient;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@Component
@Slf4j
public class NotificationServiceFallback implements NotificationServiceClient {

    @Override
    public NotificationResponse send(
            @Valid @RequestBody SendNotificationRequest req) {
        // Notifications are NON-CRITICAL — just log and swallow
        log.warn("Notification Service fallback triggered — notification skipped");
        throw new ExternalServiceException("Notification Service", "Service is unreachable");
    }
}
