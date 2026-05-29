package com.cognizant.TransactionService.service;

import com.cognizant.TransactionService.dto.*;
import com.cognizant.TransactionService.entity.LedgerEntry;
import com.cognizant.TransactionService.entity.Transaction;
import com.cognizant.TransactionService.entity.enums.EntryType;
import com.cognizant.TransactionService.entity.enums.TransactionStatus;
import com.cognizant.TransactionService.entity.enums.TransactionType;
import com.cognizant.TransactionService.entity.enums.UserRole;
import com.cognizant.TransactionService.exception.InvalidTransactionException;
import com.cognizant.TransactionService.exception.TransactionNotFoundException;
import com.cognizant.TransactionService.repository.LedgerEntryRepository;
import com.cognizant.TransactionService.repository.TransactionRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    // -----------------------------------------------------
    // INTERNAL
    // -------------------------------------------------------


    @Override
    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request, Long userId) {

        // type = transfer but target wallet id missing
        if (request.getType() == TransactionType.TRANSFER && request.getTargetWalletId() == null) {
            throw new InvalidTransactionException("Target wallet id required for TRANSFER transactions");
        }

        // type = transfer but target wallet id missing
        if (request.getType() == TransactionType.TRANSFER && (request.getTargetUsername() == null || request.getUsername() == null)) {
            throw new InvalidTransactionException("Sender username & Target username id required for TRANSFER transactions");
        }

        // type = transfer but walletId = targetWalletId
        if (request.getType() == TransactionType.TRANSFER && request.getWalletId().equals(request.getTargetWalletId())) {
            throw new InvalidTransactionException("Source and target wallet cannot be same");
        }

        // type = transfer but username = targetUsername
        if (request.getType() == TransactionType.TRANSFER && request.getUsername().equals(request.getTargetUsername())) {
            throw new InvalidTransactionException("Source and target username cannot be same");
        }

        // check if idempotencyKey already exists
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            log.info("Idempotency hit for key: {}, returning existing transaction: {}", request.getIdempotencyKey(), existing.get().getTransactionId());
            return toResponse(existing.get());
        }

        Transaction txn = Transaction.builder()
                .transactionId(generateTransactionId())
                .walletId(request.getWalletId())
                .username(request.getUsername())
                .targetWalletId(request.getTargetWalletId())
                .userId(userId)
                .targetUserId(request.getTargetUserId())
                .targetUsername(request.getTargetUsername())
                .type(request.getType())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .status(TransactionStatus.COMPLETED)
                .idempotencyKey(request.getIdempotencyKey())
                .metadata(request.getMetadata())
                .build();

        transactionRepository.save(txn);
        createLedgerEntries(txn);
        log.info("Created transaction: txnId={}, type={}, walletId={}, userId={}",
                txn.getTransactionId(), txn.getType(), txn.getWalletId(), userId);

        return toResponse(txn);
    }

    // ---------------------------------------
    // USER | ADMIN
    // ------------------------------------------

    // read by business transaction id with userId ownership validation
    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getByTransactionId(String transactionId, Long userId, UserRole role) {
        Transaction txn = transactionRepository
                .findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
        if(!role.equals(UserRole.ADMIN)){
            validateOwnership(txn, userId);
        }
        return toResponse(txn);

    }

    // --------------------------------------------
    // USER
    // --------------------------------------------

    // user read by primary key with ownership check
    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getById(Long id, Long userId) {
        Transaction txn = transactionRepository
                .findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id.toString()));
        validateOwnership(txn, userId);
        return toResponse(txn);
    }

    // user read by walletId with optional status/type filters (scoped to user)
    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<TransactionResponse> getByWalletIdWithFilters(
            Long walletId, Long userId, TransactionStatus status,
            TransactionType type, int page, int size) {
        Page<Transaction> p;
        if (status != null && type != null) {
            p = transactionRepository
                    .findByWalletIdAndUserIdAndStatusAndTypeOrderByCreatedAtDesc(
                            walletId, userId, status, type, PageRequest.of(page, size));
        } else if (status != null) {
            p = transactionRepository
                    .findByWalletIdAndUserIdAndStatusOrderByCreatedAtDesc(walletId, userId, status, PageRequest.of(page, size));
        }else if (type != null) {
            p = transactionRepository.findByWalletIdAndUserIdAndTypeOrderByCreatedAtDesc(walletId, userId, type, PageRequest.of(page, size));
        } else {
            p = transactionRepository
                    .findByWalletIdAndUserIdOrderByCreatedAtDesc(walletId, userId, PageRequest.of(page, size));
        }

        return toPaginatedResponse(p);
    }

    // -----------------------------
    // ADMIN
    // ------------------------------

    // admin read by target userId (optional status/type filters)
    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<TransactionResponse> getAdminByUserId(
            Long targetUserId, TransactionStatus status, TransactionType type, int page, int size) {
        Page<Transaction> p;
        if (status != null && type != null) {
            p = transactionRepository.findByUserIdAndStatusAndTypeOrderByCreatedAtDesc(
                    targetUserId, status, type, PageRequest.of(page, size));
        } else if (status != null) {
            p = transactionRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                    targetUserId, status, PageRequest.of(page, size));
        } else if (type != null) {
            p = transactionRepository.findByUserIdAndTypeOrderByCreatedAtDesc(
                    targetUserId, type, PageRequest.of(page, size));
        } else {
            p = transactionRepository.findByUserIdOrderByCreatedAtDesc(targetUserId, PageRequest.of(page, size));
        }
        return toPaginatedResponse(p);
    }

    // admin read by walletId (optional status/type filters)
    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<TransactionResponse> getAdminByWalletId(
            Long walletId, TransactionStatus status, TransactionType type, int page, int size) {
        Page<Transaction> p;
        if (status != null && type != null) {
            p = transactionRepository.findByWalletIdAndStatusAndTypeOrderByCreatedAtDesc(
                    walletId, status, type, PageRequest.of(page, size));
        } else if (status != null) {
            p = transactionRepository.findByWalletIdAndStatusOrderByCreatedAtDesc(
                    walletId, status, PageRequest.of(page, size));
        } else if (type != null) {
            p = transactionRepository.findByWalletIdAndTypeOrderByCreatedAtDesc(
                    walletId, type, PageRequest.of(page, size));
        } else {
            p = transactionRepository.findByWalletIdOrderByCreatedAtDesc(walletId, PageRequest.of(page, size));
        }
        return toPaginatedResponse(p);
    }


    // admin read all transactions
    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<TransactionResponse> getAllTransactions(int page, int size) {
        Page<Transaction> p = transactionRepository.findAll(
                PageRequest.of(page, size, org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt")));
        return toPaginatedResponse(p);
    }

    // ------------------------------------
    // FOR FUTURE
    // -------------------------------------

    // update status of a transaction
    @Override
    @Transactional
    public TransactionResponse updateStatus(String transactionId, TransactionStatus status) {
        Transaction txn = transactionRepository
                .findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        // GUARD: Only pending transactions can be updated
        if(txn.getStatus() != TransactionStatus.PENDING){
            throw new InvalidTransactionException(
                    "Transaction " + transactionId + " is already " + txn.getStatus()
                    + " and cannot transition to " + status);
        }

        // GUARD: COMPLETED OR FAILED transactions cannot transition to PENDING
        if(status == TransactionStatus.PENDING){
            throw new InvalidTransactionException(
                    "Cannot transition back to PENDING"
            );
        }

        txn.setStatus(status);

        // creating ledger entry once transaction completes
        if(status == TransactionStatus.COMPLETED){
            log.info("Transaction COMPLETED with ledger entries: txnId={}", transactionId);
            createLedgerEntries(txn);
        } else {
            log.warn("Transaction FAILED: txnId={}", transactionId);
        }

        transactionRepository.save(txn);
        return toResponse(txn);
    }

    // read by userId (wallets of a user)
    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<TransactionResponse> getByUserId(
            Long userId, TransactionStatus status, TransactionType type, int page, int size) {

        Page<Transaction> p;
        if (status != null && type != null) {
            p = transactionRepository.findByUserIdAndStatusAndTypeOrderByCreatedAtDesc(
                    userId, status, type, PageRequest.of(page, size));
        } else if (status != null) {
            p = transactionRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                    userId, status, PageRequest.of(page, size));
        } else if (type != null) {
            p = transactionRepository.findByUserIdAndTypeOrderByCreatedAtDesc(
                    userId, type, PageRequest.of(page, size));
        } else {
            p = transactionRepository.findByUserIdOrderByCreatedAtDesc(
                    userId, PageRequest.of(page, size));
        }
        return toPaginatedResponse(p);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionSummaryResponse getSummaryByUserId(Long userId) {

        Long walletId = transactionRepository.findWalletIdByUserId(userId)
                .orElse(null); // user may have no transactions yet

        BigDecimal topupAmount = transactionRepository
                .sumAmountByUserIdAndType(userId, TransactionType.TOPUP);
        long topupCount = transactionRepository
                .countByUserIdAndType(userId, TransactionType.TOPUP);

        BigDecimal withdrawAmount = transactionRepository
                .sumAmountByUserIdAndType(userId, TransactionType.WITHDRAW);
        long withdrawCount = transactionRepository
                .countByUserIdAndType(userId, TransactionType.WITHDRAW);

        BigDecimal transfersSentAmount = transactionRepository
                .sumAmountByUserIdAndType(userId, TransactionType.TRANSFER);
        long transfersSentCount = transactionRepository
                .countByUserIdAndType(userId, TransactionType.TRANSFER);

        BigDecimal transfersReceivedAmount = walletId != null
                ? transactionRepository.sumTransfersReceivedByWalletId(walletId)
                : BigDecimal.ZERO;
        long transfersReceivedCount = walletId != null
                ? transactionRepository.countTransfersReceivedByWalletId(walletId)
                : 0L;

        BigDecimal currentBalance = walletId != null
                ? ledgerEntryRepository.computeBalanceByWalletId(walletId)
                : BigDecimal.ZERO;

        long totalCount = transactionRepository.countByUserId(userId);

        BigDecimal netFlow = topupAmount
                .add(transfersReceivedAmount)
                .subtract(withdrawAmount)
                .subtract(transfersSentAmount);

        return TransactionSummaryResponse.builder()
                .walletId(walletId)
                .currency("INR")
                .currentBalance(currentBalance)
                .topup(new AmountCount(topupAmount, topupCount))
                .withdraw(new AmountCount(withdrawAmount, withdrawCount))
                .transfersSent(new AmountCount(transfersSentAmount, transfersSentCount))
                .transfersReceived(new AmountCount(transfersReceivedAmount, transfersReceivedCount))
                .overall(new OverallStats(totalCount, netFlow))
                .build();
    }

    // --------------------------------------------------------------------------------------------------------------

    // --------------------------
    // double entry ledger creator
    // --------------------------
    private void createLedgerEntries(Transaction txn){
        switch(txn.getType()){
            case TransactionType.TOPUP -> {
                ledgerEntryRepository.save(
                        LedgerEntry.builder()
                                .transaction(txn)
                                .walletId(txn.getWalletId())
                                .entryType(EntryType.CREDIT)
                                .amount(txn.getAmount())
                                .build()
                );
            }
            case TransactionType.WITHDRAW -> {
                ledgerEntryRepository.save(
                        LedgerEntry
                                .builder()
                                .transaction(txn)
                                .walletId(txn.getWalletId())
                                .entryType(EntryType.DEBIT)
                                .amount(txn.getAmount())
                                .build()
                );
            }
            case TransactionType.TRANSFER -> {
                ledgerEntryRepository.save(
                        LedgerEntry
                                .builder()
                                .transaction(txn)
                                .walletId(txn.getWalletId())
                                .entryType(EntryType.DEBIT)
                                .amount(txn.getAmount())
                                .build()
                );
                ledgerEntryRepository.save(
                        LedgerEntry
                                .builder()
                                .transaction(txn)
                                .walletId(txn.getTargetWalletId())
                                .entryType(EntryType.CREDIT)
                                .amount(txn.getAmount())
                                .build()
                );
            }
        }
    }

    // -------------------------
    // private helper functions
    // --------------------------
    private String generateTransactionId() {
        return "txn-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        // output sample: txn-3f8a9c2e4d1b
        // here we are taking the first 12 characters after removing four '-' from the randomly generated UUID, for better readability and this practice is still safe until the system produces a billion transactions per second.
    }

    // to build api response from normal entity
    private TransactionResponse toResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionId(transaction.getTransactionId())
                .walletId(transaction.getWalletId())
                .targetWalletId(transaction.getTargetWalletId())
                .userId(transaction.getUserId())
                .targetUserId(transaction.getTargetUserId())
                .username(transaction.getUsername())
                .targetUsername(transaction.getTargetUsername())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus())
                .metadata(transaction.getMetadata())
                .createdAt(transaction.getCreatedAt())
                .idempotencyKey(transaction.getIdempotencyKey())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }

    // to build paginated api response from a page
    private PaginatedResponse<TransactionResponse> toPaginatedResponse(Page<Transaction> p){
        return PaginatedResponse.<TransactionResponse>builder()
                .content(p.getContent().stream().map(this::toResponse).toList())
                .page(p.getNumber())
                .size(p.getSize())
                .totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .last(p.isLast())
                .build();
    }

    /**
     * Ensures the requesting user owns this transaction.
     * Prevents users from accessing other users' transaction data.
     */
    private void validateOwnership(Transaction txn, Long userId){
        if(!txn.getUserId().equals(userId)){
            log.warn("Ownership violations: userId={}, attempted to access transaction={} owned by userId={}", userId, txn.getTransactionId(), txn.getUserId());
            throw new TransactionNotFoundException(txn.getTransactionId());
        }
    }


}
