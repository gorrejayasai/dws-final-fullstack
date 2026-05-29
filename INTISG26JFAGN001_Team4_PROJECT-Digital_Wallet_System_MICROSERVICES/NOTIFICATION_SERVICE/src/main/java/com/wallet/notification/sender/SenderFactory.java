package com.wallet.notification.sender;

import com.wallet.notification.enums.NotificationChannel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SenderFactory {

    private final Map<NotificationChannel, NotificationSender> senders;

    public SenderFactory(List<NotificationSender> senderList) {
        this.senders = senderList.stream()
                .collect(Collectors.toMap(
                        NotificationSender::getChannel, Function.identity()));
    }

    public NotificationSender getSender(NotificationChannel channel) {
        NotificationSender sender = senders.get(channel);
        if (sender == null) {
            throw new UnsupportedOperationException(
                    "No sender configured for channel: " + channel);
        }
        return sender;
    }
}
