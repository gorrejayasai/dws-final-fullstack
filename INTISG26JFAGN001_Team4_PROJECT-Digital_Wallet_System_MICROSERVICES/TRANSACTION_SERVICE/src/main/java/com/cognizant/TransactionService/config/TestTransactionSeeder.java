package com.cognizant.TransactionService.config;

import com.cognizant.TransactionService.entity.LedgerEntry;
import com.cognizant.TransactionService.entity.Transaction;
import com.cognizant.TransactionService.entity.enums.EntryType;
import com.cognizant.TransactionService.entity.enums.TransactionStatus;
import com.cognizant.TransactionService.entity.enums.TransactionType;
import com.cognizant.TransactionService.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class TestTransactionSeeder implements CommandLineRunner {

    private final TransactionRepository transactionRepository;

    @Override
    public void run(String... args) {
        // Skip if transactions already exist
        if (transactionRepository.count() > 0) {
            log.info("Transactions already exist, skipping seed");
            return;
        }

        RestTemplate restTemplate = new RestTemplate();

        // Resolve userId + walletId for each username
        String[] usernames = {"alice", "bob", "charlie", "diana", "evan", "fiona", "george", "hannah", "ivan", "julia"};
        Long[] userIds   = new Long[usernames.length];
        Long[] walletIds = new Long[usernames.length];

        for (int i = 0; i < usernames.length; i++) {
            try {
                // get userId
                Map<?, ?> userResp = restTemplate.getForObject(
                        "http://localhost:8082/user/internal/by-username/" + usernames[i], Map.class);
                if (userResp == null || userResp.get("userId") == null) {
                    log.warn("User not found: {}", usernames[i]);
                    return;
                }
                userIds[i] = Long.valueOf(userResp.get("userId").toString());

                // get walletId via /wallets/by-user with X-User-Id header
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.set("X-User-Id", userIds[i].toString());
                headers.set("X-User-Role", "ADMIN");
                org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
                org.springframework.http.ResponseEntity<Map> walletResp = restTemplate.exchange(
                        "http://localhost:8084/wallets/by-user",
                        org.springframework.http.HttpMethod.GET,
                        entity,
                        Map.class);
                if (walletResp.getBody() == null || walletResp.getBody().get("id") == null) {
                    log.warn("Wallet not found for user: {}", usernames[i]);
                    return;
                }
                walletIds[i] = Long.valueOf(walletResp.getBody().get("id").toString());

            } catch (Exception e) {
                log.warn("Could not resolve user/wallet for {}: {}", usernames[i], e.getMessage());
                return;
            }
        }

        // Index helpers: alice=0, bob=1, charlie=2, diana=3, evan=4,
        //                fiona=5, george=6, hannah=7, ivan=8, julia=9

        // ── TOPUPs ──────────────────────────────────────────────────────
        seedTopup("txn-alice-top01",   usernames[0], userIds[0], walletIds[0], new BigDecimal("50000"));
        seedTopup("txn-bob-top01",     usernames[1], userIds[1], walletIds[1], new BigDecimal("40000"));
        seedTopup("txn-charlie-top01", usernames[2], userIds[2], walletIds[2], new BigDecimal("30000"));
        seedTopup("txn-diana-top01",   usernames[3], userIds[3], walletIds[3], new BigDecimal("50000"));
        seedTopup("txn-evan-top01",    usernames[4], userIds[4], walletIds[4], new BigDecimal("25000"));
        seedTopup("txn-fiona-top01",   usernames[5], userIds[5], walletIds[5], new BigDecimal("35000"));
        seedTopup("txn-george-top01",  usernames[6], userIds[6], walletIds[6], new BigDecimal("45000"));
        seedTopup("txn-hannah-top01",  usernames[7], userIds[7], walletIds[7], new BigDecimal("50000"));
        seedTopup("txn-ivan-top01",    usernames[8], userIds[8], walletIds[8], new BigDecimal("20000"));
        seedTopup("txn-julia-top01",   usernames[9], userIds[9], walletIds[9], new BigDecimal("30000"));

        // ── TRANSFERs ────────────────────────────────────────────────────
        // alice → bob
        seedTransfer("txn-alice-trf01",
                usernames[0], userIds[0], walletIds[0],
                usernames[1], userIds[1], walletIds[1],
                new BigDecimal("15000"));
        // bob → charlie
        seedTransfer("txn-bob-trf01",
                usernames[1], userIds[1], walletIds[1],
                usernames[2], userIds[2], walletIds[2],
                new BigDecimal("10000"));
        // diana → evan
        seedTransfer("txn-diana-trf01",
                usernames[3], userIds[3], walletIds[3],
                usernames[4], userIds[4], walletIds[4],
                new BigDecimal("20000"));
        // george → hannah
        seedTransfer("txn-george-trf01",
                usernames[6], userIds[6], walletIds[6],
                usernames[7], userIds[7], walletIds[7],
                new BigDecimal("25000"));
        // julia → alice
        seedTransfer("txn-julia-trf01",
                usernames[9], userIds[9], walletIds[9],
                usernames[0], userIds[0], walletIds[0],
                new BigDecimal("12000"));
        // fiona → george
        seedTransfer("txn-fiona-trf01",
                usernames[5], userIds[5], walletIds[5],
                usernames[6], userIds[6], walletIds[6],
                new BigDecimal("8000"));

        // ── WITHDRAWs ────────────────────────────────────────────────────
        seedWithdraw("txn-alice-wdr01",   usernames[0], userIds[0], walletIds[0], new BigDecimal("10000"));
        seedWithdraw("txn-charlie-wdr01", usernames[2], userIds[2], walletIds[2], new BigDecimal("5000"));
        seedWithdraw("txn-ivan-wdr01",    usernames[8], userIds[8], walletIds[8], new BigDecimal("8000"));

        log.info("Transaction seed complete.");
    }

    private void seedTopup(String txnId, String username, Long userId, Long walletId, BigDecimal amount) {
        if (transactionRepository.findByTransactionId(txnId).isPresent()) return;

        Transaction t = Transaction.builder()
                .transactionId(txnId)
                .username(username)
                .walletId(walletId)
                .userId(userId)
                .type(TransactionType.TOPUP)
                .amount(amount)
                .currency("INR")
                .status(TransactionStatus.COMPLETED)
                .idempotencyKey("idem-" + txnId)
                .createdAt(Instant.now())
                .build();

        t.getLedgerEntries().add(LedgerEntry.builder()
                .transaction(t)
                .walletId(walletId)
                .entryType(EntryType.CREDIT)
                .amount(amount)
                .build());

        transactionRepository.save(t);
        log.info("Seeded TOPUP: {}", txnId);
    }

    private void seedTransfer(String txnId,
                               String senderUsername, Long senderUserId, Long senderWalletId,
                               String receiverUsername, Long receiverUserId, Long receiverWalletId,
                               BigDecimal amount) {
        if (transactionRepository.findByTransactionId(txnId).isPresent()) return;

        Transaction t = Transaction.builder()
                .transactionId(txnId)
                .username(senderUsername)
                .walletId(senderWalletId)
                .userId(senderUserId)
                .targetUsername(receiverUsername)
                .targetWalletId(receiverWalletId)
                .targetUserId(receiverUserId)
                .type(TransactionType.TRANSFER)
                .amount(amount)
                .currency("INR")
                .status(TransactionStatus.COMPLETED)
                .idempotencyKey("idem-" + txnId)
                .createdAt(Instant.now())
                .build();

        t.getLedgerEntries().add(LedgerEntry.builder()
                .transaction(t).walletId(senderWalletId).entryType(EntryType.DEBIT).amount(amount).build());
        t.getLedgerEntries().add(LedgerEntry.builder()
                .transaction(t).walletId(receiverWalletId).entryType(EntryType.CREDIT).amount(amount).build());

        transactionRepository.save(t);
        log.info("Seeded TRANSFER: {}", txnId);
    }

    private void seedWithdraw(String txnId, String username, Long userId, Long walletId, BigDecimal amount) {
        if (transactionRepository.findByTransactionId(txnId).isPresent()) return;

        Transaction t = Transaction.builder()
                .transactionId(txnId)
                .username(username)
                .walletId(walletId)
                .userId(userId)
                .type(TransactionType.WITHDRAW)
                .amount(amount)
                .currency("INR")
                .status(TransactionStatus.COMPLETED)
                .idempotencyKey("idem-" + txnId)
                .createdAt(Instant.now())
                .build();

        t.getLedgerEntries().add(LedgerEntry.builder()
                .transaction(t)
                .walletId(walletId)
                .entryType(EntryType.DEBIT)
                .amount(amount)
                .build());

        transactionRepository.save(t);
        log.info("Seeded WITHDRAW: {}", txnId);
    }
}
