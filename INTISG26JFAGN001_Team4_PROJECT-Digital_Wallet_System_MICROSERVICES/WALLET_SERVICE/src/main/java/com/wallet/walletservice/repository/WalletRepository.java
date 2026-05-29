package com.wallet.walletservice.repository;

import com.wallet.walletservice.entity.Wallet;
import com.wallet.walletservice.enums.WalletStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    boolean existsByUserId(Long userId);

    // Returns single wallet — one wallet per user
    Optional<Wallet> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional<Wallet> findByIdWithLock(@Param("id") Long id);

    // Admin dashboard — list + filter
    List<Wallet> findByStatus(WalletStatus status);

    // Admin dashboard — summary counts
    long countByStatus(WalletStatus status);
}