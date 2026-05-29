package com.cognizant.TransactionService.entity;

import com.cognizant.TransactionService.entity.enums.TransactionStatus;
import com.cognizant.TransactionService.entity.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_txn_wallet_status", columnList = "wallet_id, status"),
        @Index(name = "idx_txn_wallet_type", columnList = "wallet_id, type"),
        @Index(name = "idx_txn_wallet_user_status_type", columnList = "wallet_id, user_id, status, type"),
        @Index(name = "idx_txn_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_txn_target_user_id", columnList = "target_user_id")  // NEW: speeds up receiver-side queries
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 64)
    private String transactionId;

    @Column(name="username")
    private String username;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Column(name="target_wallet_id")
    private Long targetWalletId;

    @Column(name = "target_username")
    private String targetUsername;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;
    //    Hibernate typically defaults to VARCHAR(255). If the enum constants are small (e.g., "TOPUP", "WITHDRAW", "TRANSFER), setting length = 20 saves storage space and improves indexing performance.

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;
    // precision is the total number of digits allowed and scale is the number of digits allowed after decimal

    @Column(length = 10)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "idempotency_key", unique = true, length = 128)
    private String idempotencyKey;

    @Column(columnDefinition = "json")
    private String metadata;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LedgerEntry> ledgerEntries = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PreUpdate
    protected void onUpdate(){
        this.updatedAt = Instant.now();
    }

}
