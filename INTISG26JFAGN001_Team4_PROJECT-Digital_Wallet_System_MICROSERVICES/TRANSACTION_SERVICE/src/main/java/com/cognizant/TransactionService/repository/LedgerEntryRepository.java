package com.cognizant.TransactionService.repository;

import com.cognizant.TransactionService.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByWalletIdOrderByCreatedAtDesc(Long walletId);

    List<LedgerEntry> findByTransactionTransactionId(Long transactionId);

    @Query("SELECT COALESCE(SUM(CASE WHEN l.entryType = 'CREDIT' THEN l.amount ELSE -l.amount END), 0) " +
            "FROM LedgerEntry l WHERE l.walletId = :walletId")
    BigDecimal computeBalanceByWalletId(@Param("walletId") Long walletId);
}
