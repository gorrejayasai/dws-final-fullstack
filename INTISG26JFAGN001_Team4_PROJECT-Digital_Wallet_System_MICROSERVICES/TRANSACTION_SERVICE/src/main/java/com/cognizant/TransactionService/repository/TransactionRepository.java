package com.cognizant.TransactionService.repository;

import com.cognizant.TransactionService.entity.Transaction;
import com.cognizant.TransactionService.entity.enums.TransactionStatus;
import com.cognizant.TransactionService.entity.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionId(String transactionId);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    // ---- User-scoped queries (used by controller endpoints) ----

    @Query("""
            SELECT t FROM Transaction t
            WHERE (t.walletId = :walletId OR t.targetWalletId = :walletId)
              AND (t.userId = :userId OR t.targetUserId = :userId)
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findByWalletIdAndUserIdOrderByCreatedAtDesc(
            @Param("walletId") Long walletId,
            @Param("userId") Long userId,
            Pageable pageable);

    @Query("""
            SELECT t FROM Transaction t
            WHERE (t.walletId = :walletId OR t.targetWalletId = :walletId)
              AND (t.userId = :userId OR t.targetUserId = :userId)
              AND t.status = :status
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findByWalletIdAndUserIdAndStatusOrderByCreatedAtDesc(
            @Param("walletId") Long walletId,
            @Param("userId") Long userId,
            @Param("status") TransactionStatus status,
            Pageable pageable
    );

    @Query("""
            SELECT t FROM Transaction t
            WHERE (t.walletId = :walletId OR t.targetWalletId = :walletId)
              AND (t.userId = :userId OR t.targetUserId = :userId)
              AND t.type = :type
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findByWalletIdAndUserIdAndTypeOrderByCreatedAtDesc(
            @Param("walletId") Long walletId,
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            Pageable pageable
    );

    @Query("""
            SELECT t FROM Transaction t
            WHERE (t.walletId = :walletId OR t.targetWalletId = :walletId)
              AND (t.userId = :userId OR t.targetUserId = :userId)
              AND t.status = :status
              AND t.type = :type
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findByWalletIdAndUserIdAndStatusAndTypeOrderByCreatedAtDesc(
            @Param("walletId") Long walletId,
            @Param("userId") Long userId,
            @Param("status") TransactionStatus status,
            @Param("type") TransactionType type,
            Pageable pageable);

    // ---- USER | Admin queries (not scoped by authenticated userId) ----

    @Query("""
            SELECT t FROM Transaction t
            WHERE (t.userId = :userId OR t.targetUserId = :userId)
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT t FROM Transaction t
            WHERE (t.userId = :userId OR t.targetUserId = :userId)
              AND t.status = :status
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findByUserIdAndStatusOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("status") TransactionStatus status,
            Pageable pageable);

    @Query("""
            SELECT t FROM Transaction t
            WHERE (t.userId = :userId OR t.targetUserId = :userId)
              AND t.type = :type
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findByUserIdAndTypeOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            Pageable pageable);

    @Query("""
            SELECT t FROM Transaction t
            WHERE (t.userId = :userId OR t.targetUserId = :userId)
              AND t.status = :status
              AND t.type = :type
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findByUserIdAndStatusAndTypeOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("status") TransactionStatus status,
            @Param("type") TransactionType type,
            Pageable pageable);

    // ---- Admin queries (not scoped by authenticated userId) ----

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.walletId = :walletId OR t.targetWalletId = :walletId
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findByWalletIdOrderByCreatedAtDesc(
            @Param("walletId") Long walletId,
            Pageable pageable
    );

    @Query("""
            SELECT t FROM Transaction t
            WHERE (t.walletId = :walletId OR t.targetWalletId = :walletId)
              AND t.status = :status
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findByWalletIdAndStatusOrderByCreatedAtDesc(
            @Param("walletId") Long walletId,
            @Param("status") TransactionStatus status,
            Pageable pageable);

    @Query("""
            SELECT t FROM Transaction t
            WHERE (t.walletId = :walletId OR t.targetWalletId = :walletId)
              AND t.type = :type
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findByWalletIdAndTypeOrderByCreatedAtDesc(
            @Param("walletId") Long walletId,
            @Param("type") TransactionType type,
            Pageable pageable);

    @Query("""
            SELECT t FROM Transaction t
            WHERE (t.walletId = :walletId OR t.targetWalletId = :walletId)
              AND t.status = :status
              AND t.type = :type
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findByWalletIdAndStatusAndTypeOrderByCreatedAtDesc(
            @Param("walletId") Long walletId,
            @Param("status") TransactionStatus status,
            @Param("type") TransactionType type,
            Pageable pageable);

    // ---- AGGREGATION QUERIES FOR DASHBOARD ----
    // Sum and count for a given userId and type (covers TOPUP, WITHDRAW, and TRANSFER-sent)
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.userId = :userId AND t.type = :type AND t.status = 'COMPLETED'")
    BigDecimal sumAmountByUserIdAndType(@Param("userId") Long userId,
                                        @Param("type") TransactionType type);

    @Query("SELECT COUNT(t) FROM Transaction t " +
            "WHERE t.userId = :userId AND t.type = :type AND t.status = 'COMPLETED'")
    long countByUserIdAndType(@Param("userId") Long userId,
                              @Param("type") TransactionType type);

    // Transfers RECEIVED — user is the target, not the initiator
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.targetWalletId = :walletId AND t.type = 'TRANSFER' AND t.status = 'COMPLETED'")
    BigDecimal sumTransfersReceivedByWalletId(@Param("walletId") Long walletId);

    @Query("SELECT COUNT(t) FROM Transaction t " +
            "WHERE t.targetWalletId = :walletId AND t.type = 'TRANSFER' AND t.status = 'COMPLETED'")
    long countTransfersReceivedByWalletId(@Param("walletId") Long walletId);

    // Total transaction count for the user (as initiator OR receiver)
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.userId = :userId OR t.targetUserId = :userId")
    long countByUserId(@Param("userId") Long userId);

    // In TransactionRepository
    @Query("SELECT DISTINCT t.walletId FROM Transaction t WHERE t.userId = :userId OR targetUserId = :userId")
    Optional<Long> findWalletIdByUserId(@Param("userId") Long userId);
}