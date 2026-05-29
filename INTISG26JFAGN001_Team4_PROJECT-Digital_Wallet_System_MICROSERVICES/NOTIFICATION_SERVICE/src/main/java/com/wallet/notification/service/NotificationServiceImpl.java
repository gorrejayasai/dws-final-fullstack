package com.wallet.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.notification.dto.NotificationResponse;
import com.wallet.notification.dto.NotificationStatsResponse;
import com.wallet.notification.dto.SendNotificationRequest;
import com.wallet.notification.entity.Notification;
import com.wallet.notification.enums.NotificationChannel;
import com.wallet.notification.enums.NotificationStatus;
import com.wallet.notification.exception.ResourceNotFoundException;
import com.wallet.notification.repository.NotificationRepository;
import com.wallet.notification.sender.SenderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl {

    private final NotificationRepository notifRepo;
    private final SenderFactory senderFactory;
    private final ObjectMapper objectMapper;

    @Transactional
    public NotificationResponse send(SendNotificationRequest req) {
        Notification notif = Notification.builder()
                .userId(req.userId())
                .channel(req.channel())
                .templateCode(req.templateCode())
                .payload(req.payload())
                .recipient(resolveRecipient(req.channel(), req.payload()))
                .build();

        notif = notifRepo.save(notif);
        return dispatch(notif);
    }

    @Transactional
    public void sendAsync(SendNotificationRequest req) {
        send(req);
    }

    @Transactional
    public void retryFailed(Notification notif) {
        if (!notif.canRetry()) {
            log.info("Notification {} exceeded max retries ({})",
                    notif.getId(), notif.getAttempts());
            return;
        }
        dispatch(notif);
    }

    public NotificationResponse getById(Long id) {
        return toResponse(notifRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification " + id + " not found.")));
    }

    public Page<NotificationResponse> getByUser(Long userId, int page, int size) {
        return notifRepo
                .findByUserId(userId,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toResponse);
    }

    public Page<NotificationResponse> getByUserAndStatus(
            Long userId, NotificationStatus status, int page, int size) {
        return notifRepo
                .findByUserIdAndStatus(userId, status,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toResponse);
    }

    public NotificationStatsResponse getStats() {
        return new NotificationStatsResponse(
                notifRepo.countByStatus(NotificationStatus.PENDING),
                notifRepo.countByStatus(NotificationStatus.SENT),
                notifRepo.countByStatus(NotificationStatus.FAILED)
        );
    }

    private NotificationResponse dispatch(Notification notif) {
        notif.incrementAttempts();
        try {
            senderFactory.getSender(notif.getChannel()).send(notif);
            notif.markSent();
            log.info("Notification {} sent via {} to {}",
                    notif.getId(), notif.getChannel(), notif.getRecipient());
        } catch (Exception e) {
            notif.markFailed(truncate(e.getMessage(), 500));
            log.error("Failed notification {} (attempt {}): {}",
                    notif.getId(), notif.getAttempts(), e.getMessage());
        }
        return toResponse(notifRepo.save(notif));
    }

    private String resolveRecipient(NotificationChannel channel, String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            return switch (channel) {
                case EMAIL -> node.has("email")       ? node.get("email").asText()       : null;
                case PUSH  -> node.has("deviceToken") ? node.get("deviceToken").asText() : null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getUserId(), n.getChannel(), n.getStatus(),
                n.getTemplateCode(), n.getRecipient(), n.getAttempts(),
                n.getLastError(), n.getCreatedAt(), n.getSentAt());
    }

    private String truncate(String s, int max) {
        return (s != null && s.length() > max) ? s.substring(0, max) : s;
    }
}
