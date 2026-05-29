package com.wallet.walletservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.walletservice.dto.request.*;
import com.wallet.walletservice.dto.response.*;
import com.wallet.walletservice.entity.Wallet;
import com.wallet.walletservice.enums.*;
import com.wallet.walletservice.exception.*;
import com.wallet.walletservice.exception.balance.*;
import com.wallet.walletservice.exception.common.*;
import com.wallet.walletservice.exception.transfer.*;
import com.wallet.walletservice.exception.wallet.*;
import com.wallet.walletservice.feign.*;
import com.wallet.walletservice.mapper.WalletMapper;
import com.wallet.walletservice.repository.WalletRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock private WalletRepository walletRepo;
    @Mock private TransactionServiceClient transactionClient;
    @Mock private NotificationServiceClient notificationClient;
    @Mock private UserServiceClient userServiceClient;

    @Spy private WalletMapper walletMapper = new WalletMapper();
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private WalletService walletService;

    private static final String USER = "USER";
    private static final String ADMIN = "ADMIN";
    private static final String RECEIVER_USERNAME = "receiver_user";
    private static final String SELF_USERNAME = "self_user";

    private Wallet active, frozen, closed, receiver;

    private Wallet buildWallet(Long id, Long userId, BigDecimal bal, WalletStatus st) {
        Wallet w = new Wallet();
        w.setId(id);
        w.setUserId(userId);
        w.setCurrency("INR");
        w.setAvailableBalance(bal);
        w.setHeldBalance(BigDecimal.ZERO);
        w.setStatus(st);
        return w;
    }

    private void stubTxnService() {
        when(transactionClient.createTransaction(any(), anyLong(), anyString()))
                .thenReturn(TransactionResponse.builder().build());
    }

    @BeforeEach
    void setUp() {
        active   = buildWallet(5L, 1005L, new BigDecimal("500000.0000"), WalletStatus.ACTIVE);
        frozen   = buildWallet(6L, 1006L, new BigDecimal("300000.0000"), WalletStatus.FROZEN);
        closed   = buildWallet(7L, 1007L, BigDecimal.ZERO, WalletStatus.CLOSED);
        receiver = buildWallet(8L, 2008L, new BigDecimal("100000.0000"), WalletStatus.ACTIVE);
    }

    // ───────── CREATE ─────────

    @Test
    void createWallet_ok() {
        when(walletRepo.existsByUserId(1005L)).thenReturn(false);
        when(walletRepo.save(any())).thenAnswer(i -> {
            Wallet w = i.getArgument(0);
            w.setId(5L);
            return w;
        });

        var r = walletService.createWallet(new CreateWalletRequest(1005L, "INR"));

        assertThat(r.id()).isEqualTo(5L);
        assertThat(r.currency()).isEqualTo("INR");
        assertThat(r.status()).isEqualTo(WalletStatus.ACTIVE);
    }

    @Test
    void createWallet_duplicate() {
        when(walletRepo.existsByUserId(1005L)).thenReturn(true);

        assertThatThrownBy(() ->
                walletService.createWallet(new CreateWalletRequest(1005L, "INR")))
                .isInstanceOf(WalletAlreadyExistsException.class);
    }

    // ───────── TOPUP ─────────

    @Test
    void topup_ok() {
        when(walletRepo.findByIdWithLock(5L)).thenReturn(Optional.of(active));
        stubTxnService();

        walletService.topup(
                5L, new TopupRequest(new BigDecimal("40000"), "INR"),
                1005L, USER);

        assertThat(active.getAvailableBalance())
                .isEqualByComparingTo("540000.0000");
    }

    @Test
    void topup_frozen() {
        when(walletRepo.findByIdWithLock(6L)).thenReturn(Optional.of(frozen));

        assertThatThrownBy(() ->
                walletService.topup(
                        6L, new TopupRequest(BigDecimal.TEN, "INR"),
                        1006L, USER))
                .isInstanceOf(WalletFrozenException.class);
    }

    // ───────── TRANSFER ─────────

    private void stubUserLookup(String username, Long userId) {
        when(userServiceClient.getUserByUsername(username))
                .thenReturn(UserLookupResponse.builder()
                        .userId(userId)
                        .username(username)
                        .status("ACTIVE")
                        .build());
    }

    @Test
    void transfer_ok() {
        stubUserLookup(RECEIVER_USERNAME, 2008L);
        when(walletRepo.findByUserId(2008L)).thenReturn(Optional.of(receiver));
        when(walletRepo.findByIdWithLock(5L)).thenReturn(Optional.of(active));
        when(walletRepo.findByIdWithLock(8L)).thenReturn(Optional.of(receiver));
        stubTxnService();

        walletService.transfer(
                5L, new TransferRequest(new BigDecimal("50000"), "INR", SELF_USERNAME, RECEIVER_USERNAME),
                1005L, USER);

        assertThat(active.getAvailableBalance())
                .isEqualByComparingTo("450000.0000");
        assertThat(receiver.getAvailableBalance())
                .isEqualByComparingTo("150000.0000");
    }

    @Test
    void transfer_self() {
        stubUserLookup(SELF_USERNAME, 1005L);

        assertThatThrownBy(() ->
                walletService.transfer(
                        5L, new TransferRequest(BigDecimal.TEN, "INR", SELF_USERNAME, SELF_USERNAME),
                        1005L, USER))
                .isInstanceOf(SelfTransferException.class);
    }

    @Test
    void transfer_recipientUsernameNotFound() {
        when(userServiceClient.getUserByUsername("ghost"))
                .thenReturn(null);

        assertThatThrownBy(() ->
                walletService.transfer(
                        5L, new TransferRequest(new BigDecimal("100"), "INR", SELF_USERNAME, "ghost"),
                        1005L, USER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void transfer_recipientHasNoWallet() {
        stubUserLookup(RECEIVER_USERNAME, 2008L);
        when(walletRepo.findByUserId(2008L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                walletService.transfer(
                        5L, new TransferRequest(new BigDecimal("100"), "INR", SELF_USERNAME, RECEIVER_USERNAME),
                        1005L, USER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ───────── WITHDRAW ─────────

    @Test
    void withdraw_ok() {
        when(walletRepo.findByIdWithLock(5L)).thenReturn(Optional.of(active));
        stubTxnService();

        walletService.withdraw(
                5L, new WithdrawRequest(new BigDecimal("40000"), "INR"),
                1005L, USER);

        assertThat(active.getAvailableBalance())
                .isEqualByComparingTo("460000.0000");
    }

    // ───────── FREEZE ─────────

    @Test
    void freeze_ok_adminOnly() {
        when(walletRepo.findByIdWithLock(5L)).thenReturn(Optional.of(active));

        assertThat(walletService.freezeWallet(5L, ADMIN).status())
                .isEqualTo(WalletStatus.FROZEN);
    }

    // ───────── UNFREEZE ─────────

    @Test
    void unfreeze_ok_adminOnly() {
        when(walletRepo.findByIdWithLock(6L)).thenReturn(Optional.of(frozen));

        assertThat(walletService.unfreezeWallet(6L, ADMIN).status())
                .isEqualTo(WalletStatus.ACTIVE);
    }

    // ───────── CLOSE ─────────

    @Test
    void close_ok() {
        active.setAvailableBalance(BigDecimal.ZERO);
        when(walletRepo.findByIdWithLock(5L)).thenReturn(Optional.of(active));

        assertThat(walletService.closeWallet(5L, 1005L, USER).status())
                .isEqualTo(WalletStatus.CLOSED);
    }

    @Test
    void close_hasBalance() {
        when(walletRepo.findByIdWithLock(5L)).thenReturn(Optional.of(active));

        assertThatThrownBy(() ->
                walletService.closeWallet(5L, 1005L, USER))
                .isInstanceOf(BusinessRuleException.class);
    }

    // ───────── QUERIES ─────────

    @Test
    void getWalletById_ok() {
        when(walletRepo.findById(5L)).thenReturn(Optional.of(active));

        assertThat(walletService.getWalletById(5L, 1005L, ADMIN).id())
                .isEqualTo(5L);
    }

    @Test
    void getWalletByUserId_ok() {
        when(walletRepo.findByUserId(1005L)).thenReturn(Optional.of(active));

        assertThat(walletService.getWalletByUserId(1005L).id())
                .isEqualTo(5L);
    }

    @Test
    void getBalance_ok() {
        when(walletRepo.findById(5L)).thenReturn(Optional.of(active));

        assertThat(walletService.getBalance(5L,1005L,USER).availableBalance())
                .isEqualByComparingTo("500000.0000");
    }

    // ───────── ADMIN: GET ALL ─────────

    @Test
    void getAllWallets_noFilter_returnsEveryone() {
        when(walletRepo.findAll())
                .thenReturn(List.of(active, frozen, closed, receiver));

        var result = walletService.getAllWallets(null);

        assertThat(result).hasSize(4);
        verify(walletRepo).findAll();
        verify(walletRepo, never()).findByStatus(any());
    }

    @Test
    void getAllWallets_blankFilter_returnsEveryone() {
        when(walletRepo.findAll()).thenReturn(List.of(active, frozen));

        var result = walletService.getAllWallets("   ");

        assertThat(result).hasSize(2);
        verify(walletRepo).findAll();
    }

    @Test
    void getAllWallets_statusFilter_active() {
        when(walletRepo.findByStatus(WalletStatus.ACTIVE))
                .thenReturn(List.of(active, receiver));

        var result = walletService.getAllWallets("ACTIVE");

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(w -> w.status() == WalletStatus.ACTIVE);
        verify(walletRepo).findByStatus(WalletStatus.ACTIVE);
        verify(walletRepo, never()).findAll();
    }

    @Test
    void getAllWallets_statusFilter_caseInsensitive() {
        when(walletRepo.findByStatus(WalletStatus.FROZEN))
                .thenReturn(List.of(frozen));

        var result = walletService.getAllWallets("frozen");

        assertThat(result).hasSize(1);
        verify(walletRepo).findByStatus(WalletStatus.FROZEN);
    }

    @Test
    void getAllWallets_invalidStatus_throws() {
        assertThatThrownBy(() -> walletService.getAllWallets("BANANA"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Unknown wallet status");

        verify(walletRepo, never()).findAll();
        verify(walletRepo, never()).findByStatus(any());
    }

    // ───────── ADMIN: SUMMARY ─────────

    @Test
    void getSummary_ok() {
        when(walletRepo.countByStatus(WalletStatus.ACTIVE)).thenReturn(8L);
        when(walletRepo.countByStatus(WalletStatus.FROZEN)).thenReturn(3L);
        when(walletRepo.countByStatus(WalletStatus.CLOSED)).thenReturn(1L);

        var summary = walletService.getSummary();

        assertThat(summary.total()).isEqualTo(12L);
        assertThat(summary.active()).isEqualTo(8L);
        assertThat(summary.frozen()).isEqualTo(3L);
        assertThat(summary.closed()).isEqualTo(1L);
    }

    @Test
    void getSummary_emptyDb_returnsZeros() {
        when(walletRepo.countByStatus(any())).thenReturn(0L);

        var summary = walletService.getSummary();

        assertThat(summary.total()).isZero();
        assertThat(summary.active()).isZero();
        assertThat(summary.frozen()).isZero();
        assertThat(summary.closed()).isZero();
    }
}