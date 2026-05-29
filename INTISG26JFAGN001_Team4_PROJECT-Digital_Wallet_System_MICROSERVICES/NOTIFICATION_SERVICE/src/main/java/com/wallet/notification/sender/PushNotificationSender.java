package com.wallet.notification.sender;

import com.wallet.notification.entity.Notification;
import com.wallet.notification.enums.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PushNotificationSender implements NotificationSender {

    @Override
    public NotificationChannel getChannel() { return NotificationChannel.PUSH; }

    @Override
    public void send(Notification notification) {
        log.info("Push sent to {} [{}]",
                notification.getRecipient(), notification.getTemplateCode());
    }
}
