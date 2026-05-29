package com.wallet.notification.scheduler;

import com.wallet.notification.enums.NotificationStatus;
import com.wallet.notification.repository.NotificationRepository;
import com.wallet.notification.service.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRetryScheduler {

    private final NotificationRepository notifRepo;
    private final NotificationServiceImpl notifService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void retryFailedNotifications() {
        var since = Instant.now().minus(24, ChronoUnit.HOURS);
        var retryable = notifRepo.findRetryable(NotificationStatus.FAILED, since);
        if (!retryable.isEmpty()) {
            log.info("Retrying {} failed notifications", retryable.size());
            retryable.forEach(notifService::retryFailed);
        }
    }
}