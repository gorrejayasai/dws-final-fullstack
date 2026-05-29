package com.wallet.notification.repository;

import com.wallet.notification.entity.Notification;
import com.wallet.notification.enums.NotificationChannel;
import com.wallet.notification.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserId(Long userId, Pageable pageable);

    Page<Notification> findByUserIdAndStatus(Long userId, NotificationStatus status, Pageable pageable);

    Page<Notification> findByUserIdAndChannel(Long userId, NotificationChannel channel, Pageable pageable);

    @Query("""
        SELECT n FROM Notification n
        WHERE n.status = :status
          AND n.attempts < 5
          AND n.createdAt > :since
        ORDER BY n.createdAt ASC
    """)
    List<Notification> findRetryable(
            @Param("status") NotificationStatus status,
            @Param("since")  Instant since
    );

    long countByStatus(NotificationStatus status);
}