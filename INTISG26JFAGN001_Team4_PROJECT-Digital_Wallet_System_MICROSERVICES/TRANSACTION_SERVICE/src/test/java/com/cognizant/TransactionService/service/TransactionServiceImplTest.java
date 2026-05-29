package com.cognizant.TransactionService.service;

import com.cognizant.TransactionService.dto.CreateTransactionRequest;
import com.cognizant.TransactionService.dto.PaginatedResponse;
import com.cognizant.TransactionService.dto.TransactionResponse;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Test
    void createTransaction_transferWithoutTargetWallet_throwsInvalidTransactionException() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                101L, null, null, null, null,
                TransactionType.TRANSFER,
                new BigDecimal("100.00"), "INR", "idem-001", null
        );

        assertThrows(InvalidTransactionException.class, () -> transactionService.createTransaction(request, 1L));
        verifyNoInteractions(transactionRepository, ledgerEntryRepository);
    }

    @Test
    void createTransaction_whenIdempotencyKeyExists_returnsExistingTransaction() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                101L, null, null, null, null,
                TransactionType.TOPUP,
                new BigDecimal("100.00"), "INR", "idem-002", null
        );

        Transaction existing = Transaction.builder()
                .id(10L)
                .transactionId("txn-existing-01")
                .walletId(101L)
                .userId(1L)
                .type(TransactionType.TOPUP)
                .amount(new BigDecimal("100.00"))
                .currency("INR")
                .status(TransactionStatus.COMPLETED)
                .idempotencyKey("idem-002")
                .build();

        when(transactionRepository.findByIdempotencyKey("idem-002")).thenReturn(Optional.of(existing));

        TransactionResponse response = transactionService.createTransaction(request, 1L);

        assertEquals("txn-existing-01", response.getTransactionId());
        assertEquals(TransactionStatus.COMPLETED, response.getStatus());
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(ledgerEntryRepository, never()).save(any(LedgerEntry.class));
    }

    @Test
    void createTransaction_topup_savesTransactionAndCreditLedgerEntry() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                101L, null, null, null, null,
                TransactionType.TOPUP,
                new BigDecimal("250.00"),
                "INR", "idem-003", "test-topup"
        );

        when(transactionRepository.findByIdempotencyKey("idem-003")).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transactionService.createTransaction(request, 99L);

        ArgumentCaptor<Transaction> txnCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txnCaptor.capture());

        Transaction savedTxn = txnCaptor.getValue();
        assertNotNull(savedTxn.getTransactionId());
        assertEquals("INR", savedTxn.getCurrency());
        assertEquals(TransactionStatus.COMPLETED, savedTxn.getStatus());

        ArgumentCaptor<LedgerEntry> ledgerCaptor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository).save(ledgerCaptor.capture());

        LedgerEntry ledgerEntry = ledgerCaptor.getValue();
        assertEquals(101L, ledgerEntry.getWalletId());
        assertEquals(EntryType.CREDIT, ledgerEntry.getEntryType());
        assertEquals(new BigDecimal("250.00"), ledgerEntry.getAmount());

        assertEquals(TransactionType.TOPUP, response.getType());
        assertEquals(TransactionStatus.COMPLETED, response.getStatus());
    }

    @Test
    void updateStatus_fromPendingToCompleted_updatesAndCreatesLedger() {
        Transaction pendingTxn = Transaction.builder()
                .transactionId("txn-pending-01")
                .walletId(501L)
                .userId(7L)
                .type(TransactionType.WITHDRAW)
                .amount(new BigDecimal("50.00"))
                .currency("INR")
                .status(TransactionStatus.PENDING)
                .idempotencyKey("idem-004")
                .build();

        when(transactionRepository.findByTransactionId("txn-pending-01")).thenReturn(Optional.of(pendingTxn));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transactionService.updateStatus("txn-pending-01", TransactionStatus.COMPLETED);

        assertEquals(TransactionStatus.COMPLETED, pendingTxn.getStatus());
        assertEquals(TransactionStatus.COMPLETED, response.getStatus());
        verify(transactionRepository).save(pendingTxn);

        ArgumentCaptor<LedgerEntry> ledgerCaptor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository).save(ledgerCaptor.capture());
        assertEquals(EntryType.DEBIT, ledgerCaptor.getValue().getEntryType());
    }

    @Test
    void updateStatus_whenTransactionIsNotPending_throwsInvalidTransactionException() {
        Transaction completedTxn = Transaction.builder()
                .transactionId("txn-done-01")
                .walletId(700L)
                .userId(5L)
                .type(TransactionType.TOPUP)
                .amount(new BigDecimal("90.00"))
                .currency("INR")
                .status(TransactionStatus.COMPLETED)
                .idempotencyKey("idem-005")
                .build();

        when(transactionRepository.findByTransactionId("txn-done-01")).thenReturn(Optional.of(completedTxn));

        assertThrows(
                InvalidTransactionException.class,
                () -> transactionService.updateStatus("txn-done-01", TransactionStatus.FAILED)
        );

        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(ledgerEntryRepository, never()).save(any(LedgerEntry.class));
    }

    // ---------------------------------------------------------------
    // createTransaction — additional scenarios
    // ---------------------------------------------------------------

    @Test
    void createTransaction_transferWithSameSourceAndTarget_throwsInvalidTransactionException() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                101L, null, 101L, null, null,
                TransactionType.TRANSFER,
                new BigDecimal("200.00"), "INR", "idem-006", null
        );

        assertThrows(InvalidTransactionException.class,
                () -> transactionService.createTransaction(request, 1L));
        verifyNoInteractions(transactionRepository, ledgerEntryRepository);
    }

    @Test
    void createTransaction_withdraw_savesTransactionAndDebitLedgerEntry() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                200L, null, null, null, null,
                TransactionType.WITHDRAW,
                new BigDecimal("75.00"), "INR", "idem-007", null
        );

        when(transactionRepository.findByIdempotencyKey("idem-007")).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        transactionService.createTransaction(request, 10L);

        ArgumentCaptor<LedgerEntry> ledgerCaptor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository).save(ledgerCaptor.capture());
        assertEquals(EntryType.DEBIT, ledgerCaptor.getValue().getEntryType());
        assertEquals(200L, ledgerCaptor.getValue().getWalletId());
    }

    @Test
    void createTransaction_transfer_savesTwoLedgerEntries_debitSourceCreditTarget() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                300L, null, 400L, null, null,
                TransactionType.TRANSFER,
                new BigDecimal("500.00"), "INR", "idem-008", null
        );

        when(transactionRepository.findByIdempotencyKey("idem-008")).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        transactionService.createTransaction(request, 20L);

        ArgumentCaptor<LedgerEntry> ledgerCaptor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository, org.mockito.Mockito.times(2)).save(ledgerCaptor.capture());

        List<LedgerEntry> entries = ledgerCaptor.getAllValues();
        assertEquals(EntryType.DEBIT,  entries.get(0).getEntryType()); // source debited
        assertEquals(300L,             entries.get(0).getWalletId());
        assertEquals(EntryType.CREDIT, entries.get(1).getEntryType()); // target credited
        assertEquals(400L,             entries.get(1).getWalletId());
    }

    @Test
    void createTransaction_currencyDefaultsToINR_whenNotProvided() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                500L, null, null, null, null,
                TransactionType.TOPUP,
                new BigDecimal("50.00"), null, "idem-009", null
        );

        when(transactionRepository.findByIdempotencyKey("idem-009")).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        TransactionResponse response = transactionService.createTransaction(request, 30L);

        assertEquals("INR", response.getCurrency());
    }

    // ---------------------------------------------------------------
    // updateStatus — additional scenarios
    // ---------------------------------------------------------------

    @Test
    void updateStatus_pendingToFailed_savesWithoutCreatingLedgerEntries() {
        Transaction pendingTxn = Transaction.builder()
                .transactionId("txn-fail-01")
                .walletId(600L).userId(8L)
                .type(TransactionType.TOPUP)
                .amount(new BigDecimal("30.00")).currency("INR")
                .status(TransactionStatus.PENDING)
                .idempotencyKey("idem-010")
                .build();

        when(transactionRepository.findByTransactionId("txn-fail-01")).thenReturn(Optional.of(pendingTxn));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        TransactionResponse response = transactionService.updateStatus("txn-fail-01", TransactionStatus.FAILED);

        assertEquals(TransactionStatus.FAILED, response.getStatus());
        verify(ledgerEntryRepository, never()).save(any(LedgerEntry.class));
    }

    @Test
    void updateStatus_backToPending_throwsInvalidTransactionException() {
        Transaction pendingTxn = Transaction.builder()
                .transactionId("txn-repend-01")
                .walletId(700L).userId(9L)
                .type(TransactionType.TOPUP)
                .amount(new BigDecimal("10.00")).currency("INR")
                .status(TransactionStatus.PENDING)
                .idempotencyKey("idem-011")
                .build();

        when(transactionRepository.findByTransactionId("txn-repend-01")).thenReturn(Optional.of(pendingTxn));

        // Trying to update a PENDING transaction back to PENDING should throw
        assertThrows(InvalidTransactionException.class,
                () -> transactionService.updateStatus("txn-repend-01", TransactionStatus.PENDING));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void updateStatus_transactionNotFound_throwsTransactionNotFoundException() {
        when(transactionRepository.findByTransactionId("txn-missing")).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class,
                () -> transactionService.updateStatus("txn-missing", TransactionStatus.COMPLETED));
    }

    // ---------------------------------------------------------------
    // RBAC — getByTransactionId
    // ---------------------------------------------------------------

    @Test
    void getByTransactionId_asUser_ownerCanAccess() {
        Transaction txn = Transaction.builder()
                .transactionId("txn-rbac-01")
                .walletId(800L).userId(42L)
                .type(TransactionType.TOPUP)
                .amount(new BigDecimal("100.00")).currency("INR")
                .status(TransactionStatus.COMPLETED)
                .idempotencyKey("idem-012")
                .build();

        when(transactionRepository.findByTransactionId("txn-rbac-01")).thenReturn(Optional.of(txn));

        TransactionResponse response = transactionService.getByTransactionId("txn-rbac-01", 42L, UserRole.USER);

        assertEquals("txn-rbac-01", response.getTransactionId());
    }

    @Test
    void getByTransactionId_asUser_nonOwnerCannotAccess_throwsTransactionNotFoundException() {
        Transaction txn = Transaction.builder()
                .transactionId("txn-rbac-02")
                .walletId(800L).userId(42L)   // owned by user 42
                .type(TransactionType.TOPUP)
                .amount(new BigDecimal("100.00")).currency("INR")
                .status(TransactionStatus.COMPLETED)
                .idempotencyKey("idem-013")
                .build();

        when(transactionRepository.findByTransactionId("txn-rbac-02")).thenReturn(Optional.of(txn));

        // user 99 tries to access user 42's transaction
        assertThrows(TransactionNotFoundException.class,
                () -> transactionService.getByTransactionId("txn-rbac-02", 99L, UserRole.USER));
    }

    @Test
    void getByTransactionId_asAdmin_canAccessAnyUsersTransaction() {
        Transaction txn = Transaction.builder()
                .transactionId("txn-rbac-03")
                .walletId(800L).userId(42L)   // owned by user 42
                .type(TransactionType.TOPUP)
                .amount(new BigDecimal("100.00")).currency("INR")
                .status(TransactionStatus.COMPLETED)
                .idempotencyKey("idem-014")
                .build();

        when(transactionRepository.findByTransactionId("txn-rbac-03")).thenReturn(Optional.of(txn));

        // admin with userId 1 accessing another user's transaction — should succeed
        TransactionResponse response = transactionService.getByTransactionId("txn-rbac-03", 1L, UserRole.ADMIN);

        assertEquals("txn-rbac-03", response.getTransactionId());
        assertEquals(42L, response.getUserId());
    }

    // ---------------------------------------------------------------
    // RBAC — getByWalletIdWithFilters (filter routing)
    // ---------------------------------------------------------------

    @Test
    void getByWalletIdWithFilters_noFilters_callsUnfilteredQuery() {
        when(transactionRepository.findByWalletIdAndUserIdOrderByCreatedAtDesc(
                eq(10L), eq(5L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PaginatedResponse<TransactionResponse> response =
                transactionService.getByWalletIdWithFilters(10L, 5L, null, null, 0, 20);

        verify(transactionRepository).findByWalletIdAndUserIdOrderByCreatedAtDesc(
                eq(10L), eq(5L), any(Pageable.class));
        assertEquals(0, response.getTotalElements());
    }

    @Test
    void getByWalletIdWithFilters_statusOnly_callsStatusFilterQuery() {
        when(transactionRepository.findByWalletIdAndUserIdAndStatusOrderByCreatedAtDesc(
                eq(10L), eq(5L), eq(TransactionStatus.COMPLETED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        transactionService.getByWalletIdWithFilters(10L, 5L, TransactionStatus.COMPLETED, null, 0, 20);

        verify(transactionRepository).findByWalletIdAndUserIdAndStatusOrderByCreatedAtDesc(
                eq(10L), eq(5L), eq(TransactionStatus.COMPLETED), any(Pageable.class));
    }

    @Test
    void getByWalletIdWithFilters_typeOnly_callsTypeFilterQuery() {
        when(transactionRepository.findByWalletIdAndUserIdAndTypeOrderByCreatedAtDesc(
                eq(10L), eq(5L), eq(TransactionType.TOPUP), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        transactionService.getByWalletIdWithFilters(10L, 5L, null, TransactionType.TOPUP, 0, 20);

        verify(transactionRepository).findByWalletIdAndUserIdAndTypeOrderByCreatedAtDesc(
                eq(10L), eq(5L), eq(TransactionType.TOPUP), any(Pageable.class));
    }

    @Test
    void getByWalletIdWithFilters_bothFilters_callsStatusAndTypeQuery() {
        when(transactionRepository.findByWalletIdAndUserIdAndStatusAndTypeOrderByCreatedAtDesc(
                eq(10L), eq(5L), eq(TransactionStatus.COMPLETED), eq(TransactionType.WITHDRAW), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        transactionService.getByWalletIdWithFilters(10L, 5L, TransactionStatus.COMPLETED, TransactionType.WITHDRAW, 0, 20);

        verify(transactionRepository).findByWalletIdAndUserIdAndStatusAndTypeOrderByCreatedAtDesc(
                eq(10L), eq(5L), eq(TransactionStatus.COMPLETED), eq(TransactionType.WITHDRAW), any(Pageable.class));
    }
}

