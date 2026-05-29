package com.wallet.notification.controller;

import com.wallet.notification.dto.NotificationResponse;
import com.wallet.notification.dto.NotificationStatsResponse;
import com.wallet.notification.dto.SendNotificationRequest;
import com.wallet.notification.enums.NotificationStatus;
import com.wallet.notification.service.NotificationServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notify")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Send and query notifications")
public class NotificationController {

    private final NotificationServiceImpl notifService;

    @PostMapping("/send")
    @Operation(summary = "Send a notification synchronously")
    public ResponseEntity<NotificationResponse> send(
            @Valid @RequestBody SendNotificationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notifService.send(req));
    }

    @PostMapping("/send-async")
    @Operation(summary = "Send a notification asynchronously (fire-and-forget)")
    public ResponseEntity<Void> sendAsync(
            @Valid @RequestBody SendNotificationRequest req) {
        notifService.sendAsync(req);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notification by ID")
    public ResponseEntity<NotificationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(notifService.getById(id));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "List notifications for a user (paginated)")
    public ResponseEntity<Page<NotificationResponse>> getByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notifService.getByUser(userId, page, size));
    }

    @GetMapping("/user/{userId}/status/{status}")
    @Operation(summary = "List notifications for a user filtered by status")
    public ResponseEntity<Page<NotificationResponse>> getByUserAndStatus(
            @PathVariable Long userId,
            @PathVariable NotificationStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                notifService.getByUserAndStatus(userId, status, page, size));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get notification counts by status")
    public ResponseEntity<NotificationStatsResponse> getStats() {
        return ResponseEntity.ok(notifService.getStats());
    }
}