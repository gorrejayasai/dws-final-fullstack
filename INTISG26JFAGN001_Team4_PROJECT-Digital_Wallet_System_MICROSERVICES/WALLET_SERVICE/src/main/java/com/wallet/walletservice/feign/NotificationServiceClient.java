package com.wallet.walletservice.feign;

import com.wallet.walletservice.dto.request.SendNotificationRequest;
import com.wallet.walletservice.dto.response.NotificationResponse;
import com.wallet.walletservice.feign.fallback.NotificationServiceFallback;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
        name = "notification-service",
//        url = "${feign.client.config.notification-service.url}",
        fallback = NotificationServiceFallback.class
)
public interface NotificationServiceClient {

    @PostMapping("/notify/send")
    public NotificationResponse send(
            @Valid @RequestBody SendNotificationRequest req);
}
