package com.wallet.notification.sender;

import com.wallet.notification.entity.Notification;
import com.wallet.notification.enums.NotificationChannel;

public interface NotificationSender {
    NotificationChannel getChannel();
    void send(Notification notification) throws Exception;
}