package com.wallet.notification.entity;

import com.wallet.notification.enums.NotificationChannel;
import com.wallet.notification.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notif_user_id",    columnList = "userId"),
        @Index(name = "idx_notif_status",     columnList = "status"),
        @Index(name = "idx_notif_created_at", columnList = "createdAt")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    public static final int MAX_ATTEMPTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(nullable = false, length = 50)
    private String templateCode;

    @Column(columnDefinition = "JSON")
    private String payload;

    @Column(length = 200)
    private String recipient;

    @Builder.Default
    private int attempts = 0;

    @Column(length = 500)
    private String lastError;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant updatedAt;
    private Instant sentAt;

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    public boolean canRetry() {
        return this.attempts < MAX_ATTEMPTS
                && this.status != NotificationStatus.SENT;
    }

    public void markSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt  = Instant.now();
        this.lastError = null;
    }

    public void markFailed(String error) {
        this.status    = NotificationStatus.FAILED;
        this.lastError = error;
    }

    public void incrementAttempts() {
        this.attempts++;
    }
}