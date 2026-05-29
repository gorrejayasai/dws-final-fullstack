package com.wallet.walletservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.walletservice.dto.request.*;
import com.wallet.walletservice.dto.response.*;
import com.wallet.walletservice.entity.Wallet;
import com.wallet.walletservice.enums.*;
import com.wallet.walletservice.exception.*;
import com.wallet.walletservice.exception.balance.InsufficientBalanceException;
import com.wallet.walletservice.exception.balance.InvalidAmountException;
import com.wallet.walletservice.exception.common.BusinessRuleException;
import com.wallet.walletservice.exception.common.DeniedAccessException;
import com.wallet.walletservice.exception.common.ResourceNotFoundException;
import com.wallet.walletservice.exception.currency.CurrencyMismatchException;
import com.wallet.walletservice.exception.currency.UnsupportedCurrencyException;
import com.wallet.walletservice.exception.limits.MaxBalanceExceededException;
import com.wallet.walletservice.exception.transfer.SelfTransferException;
import com.wallet.walletservice.exception.wallet.*;
import com.wallet.walletservice.feign.*;
import com.wallet.walletservice.mapper.WalletMapper;
import com.wallet.walletservice.repository.WalletRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepo;
    private final WalletMapper walletMapper;
    private final TransactionServiceClient transactionClient;
    private final NotificationServiceClient notificationClient;
    private final UserServiceClient userServiceClient;
    private final ObjectMapper objectMapper;

    // CREATE
    @Transactional
    public WalletResponse createWallet(CreateWalletRequest req) {
        log.debug("Creating wallet: userId={}, currency={}",
                req.getUserId(), req.getCurrency());

        validateSupportedCurrency(req.getCurrency());

        if (walletRepo.existsByUserId(req.getUserId())) {
            throw new WalletAlreadyExistsException(req.getUserId());
        }

        Wallet wallet = walletMapper.toEntity(req);
        wallet = walletRepo.save(wallet);
        log.info("Wallet created: id={}, userId={}, currency={}",
                wallet.getId(), req.getUserId(), req.getCurrency());

        notify(req.getUserId(), "WALLET_CREATED",
                Map.of("walletId", wallet.getId().toString()));
        return walletMapper.toResponse(wallet);
    }

    // TOPUP
    @Transactional
    public TransactionResponse topup(Long walletId, TopupRequest req, Long userId, String role) {
        log.debug("Topup: walletId={}, amount={}", walletId, req.amount());

        // topup request can be posted by a User only, Admins cannot perform topup
        if (!role.equals("USER")) {
            throw new DeniedAccessException("Admin tried to TOPUP user wallet");
        }

        Wallet wallet = lockWallet(walletId);

        // the user requesting the topup should be the wallet's owner
        if (!wallet.getUserId().equals(userId)) {
            throw new BusinessRuleException("USER_AND_WALLET_MISMATCH",
                    "WALLET: " + wallet.getId() + ", DOES NOT BELONG TO USER: " + userId);
        }

        validateActive(wallet);
        validateOperationCurrency(wallet.getCurrency(), req.currency());

        SupportedCurrency limits = SupportedCurrency.fromCode(wallet.getCurrency());

        validateAmountLimit(
                req.amount(), limits.getMaxTopupAmount(),
                "topup", wallet.getCurrency());

        BigDecimal newBalance = wallet.getAvailableBalance().add(req.amount());

        if (newBalance.compareTo(limits.getMaxBalance()) > 0) {
            throw new MaxBalanceExceededException(
                    walletId, limits.getMaxBalance(), newBalance);
        }

        String idempotencyKey = UUID.randomUUID().toString();
        log.debug("Generated idempotencyKey for topup: walletId={}, key={}",
                walletId, idempotencyKey);

        TransactionResponse txn = recordTransaction(
                walletId, wallet.getUserId(), null,
                TransactionType.TOPUP, req.amount(), wallet.getCurrency(), null,
                null, null, idempotencyKey);

        wallet.setAvailableBalance(newBalance);
        walletRepo.save(wallet);
        log.info("Topup done: walletId={}, amount={}, balance={}",
                walletId, req.amount(), newBalance);

        notify(wallet.getUserId(), "TOPUP_SUCCESS",
                Map.of("walletId", walletId, "amount", req.amount()));

        return txn;
    }

    // TRANSFER
    @Transactional
    public TransactionResponse transfer(Long sourceId, TransferRequest req, Long userId, String role) {
        log.debug("Transfer: from={}, toUsername={}, amount={}",
                sourceId, req.targetUsername(), req.amount());

        // transfer request can be posted by a User only, Admins cannot perform topup
        if (!role.equals("USER")) {
            throw new DeniedAccessException("ADMIN tried to TRANSFER from USER wallet");
        }

        // Resolve recipient username -> userId -> walletId BEFORE locking, to avoid
        // holding DB row locks while waiting on USER_SERVICE / wallet lookup.
        Long targetUserId = resolveRecipientUserId(req.targetUsername());

        // Self-transfer guard on user identity (more accurate than wallet ID compare)
        if (userId.equals(targetUserId)) {
            throw new SelfTransferException(sourceId);
        }

        Wallet targetWallet = walletRepo.findByUserId(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Recipient '" + req.targetUsername() + "' has no wallet"));
        Long targetWalletId = targetWallet.getId();

        // we do the following to prevent deadlocks, by locking wallets in a consistent manner - smallest first then largest
        // so if any other concurrent transaction occurs, they wait, otherwise locking wallets in a random manner may result to deadlock
        // 1. determine which wallet is smaller
        // 2. always lock in the same order (Smallest id first)
        // 3. after locking identify which is the sender and which is the receiver based on original request
        Long firstId = Math.min(sourceId, targetWalletId);
        Long secondId = Math.max(sourceId, targetWalletId);
        Wallet w1 = lockWallet(firstId);
        Wallet w2 = lockWallet(secondId);

        Wallet sender = w1.getId().equals(sourceId) ? w1 : w2;
        Wallet receiver = w1.getId().equals(targetWalletId) ? w1 : w2;

        // the user requesting the transfer should be the sender wallet's owner
        if (!sender.getUserId().equals(userId)) {
            throw new BusinessRuleException("USER_AND_WALLET_MISMATCH",
                    "WALLET: " + sender.getId() + ", DOES NOT BELONG TO USER: " + userId);
        }

        validateActive(sender);
        validateActive(receiver);

        validateOperationCurrency(sender.getCurrency(), req.currency());
        validateOperationCurrency(receiver.getCurrency(), req.currency());

        if (sender.getAvailableBalance().compareTo(req.amount()) < 0) {
            throw new InsufficientBalanceException(
                    sourceId, sender.getAvailableBalance(), req.amount());
        }

        SupportedCurrency senderLimits = SupportedCurrency.fromCode(sender.getCurrency());
        validateAmountLimit(
                req.amount(), senderLimits.getMaxTransferAmount(),
                "transfer", sender.getCurrency());

        SupportedCurrency receiverLimits = SupportedCurrency.fromCode(receiver.getCurrency());
        BigDecimal receiverNew = receiver.getAvailableBalance().add(req.amount());
        if (receiverNew.compareTo(receiverLimits.getMaxBalance()) > 0) {
            throw new MaxBalanceExceededException(
                    targetWalletId,
                    receiverLimits.getMaxBalance(), receiverNew);
        }

        String idempotencyKey = UUID.randomUUID().toString();
        log.debug("Generated idempotencyKey for transfer: sourceId={}, key={}",
                sourceId, idempotencyKey);

        TransactionResponse txn = recordTransaction(
                sourceId, sender.getUserId(), req.username(),
                TransactionType.TRANSFER, req.amount(), sender.getCurrency(),
                receiver.getUserId(),
                targetWalletId, req.targetUsername(), idempotencyKey);

        sender.setAvailableBalance(
                sender.getAvailableBalance().subtract(req.amount()));
        receiver.setAvailableBalance(receiverNew);
        walletRepo.save(sender);
        walletRepo.save(receiver);
        log.info("Transfer done: from={}, to={} (username={}), amount={}",
                sourceId, targetWalletId, req.targetUsername(), req.amount());

        notify(sender.getUserId(), "TRANSFER_SENT",
                Map.of("walletId", sourceId,
                        "targetWalletId", targetWalletId,
                        "amount", req.amount()));
        notify(receiver.getUserId(), "TRANSFER_RECEIVED",
                Map.of("walletId", targetWalletId,
                        "sourceWalletId", sourceId,
                        "amount", req.amount()));
        return txn;
    }

    // Resolves a username to userId via USER-SERVICE. Throws ResourceNotFoundException
    // if the recipient doesn't exist; ExternalServiceException bubbles up if USER-SERVICE is down.
    private Long resolveRecipientUserId(String username) {
        try {
            UserLookupResponse lookup = userServiceClient.getUserByUsername(username);
            if (lookup == null || lookup.getUserId() == null) {
                throw new ResourceNotFoundException("User " + username + " not found");
            }
            return lookup.getUserId();
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("User " + username + " not found");
        } catch (ExternalServiceException e) {
            throw e;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException("User Service", e.getMessage());
        }
    }

    // WITHDRAW
    @Transactional
    public TransactionResponse withdraw(Long walletId, WithdrawRequest req, Long userId, String role) {
        log.debug("Withdraw: walletId={}, amount={}", walletId, req.amount());

        // transfer request can be posted by a User only, Admins cannot perform topup
        if (!role.equals("USER")) {
            throw new DeniedAccessException("ADMIN tried to WITHDRAW from USER wallet");
        }

        Wallet wallet = lockWallet(walletId);

        // the user requesting the with-draw should be the wallet's owner
        if (!wallet.getUserId().equals(userId)) {
            throw new BusinessRuleException("USER_AND_WALLET_MISMATCH",
                    "WALLET: " + wallet.getId() + ", DOES NOT BELONG TO USER: " + userId);
        }

        validateActive(wallet);
        validateOperationCurrency(wallet.getCurrency(), req.currency());

        SupportedCurrency limits = SupportedCurrency.fromCode(wallet.getCurrency());

        if (wallet.getAvailableBalance().compareTo(req.amount()) < 0) {
            throw new InsufficientBalanceException(
                    walletId, wallet.getAvailableBalance(), req.amount());
        }

        validateAmountLimit(
                req.amount(), limits.getMaxWithdrawAmount(),
                "withdrawal", wallet.getCurrency());

        //idempotency key is used to avoid duplicate operations during any failures -ensures data consistency
        String idempotencyKey = UUID.randomUUID().toString();
        log.debug("Generated idempotencyKey for withdraw: walletId={}, key={}",
                walletId, idempotencyKey);

        TransactionResponse txn = recordTransaction(
                walletId, wallet.getUserId(), null,
                TransactionType.WITHDRAW, req.amount(), wallet.getCurrency(), null,
                null, null, idempotencyKey);

        wallet.setAvailableBalance(
                wallet.getAvailableBalance().subtract(req.amount()));
        walletRepo.save(wallet);
        log.info("Withdraw done: walletId={}, amount={}, balance={}",
                walletId, req.amount(), wallet.getAvailableBalance());

        notify(wallet.getUserId(), "WITHDRAW_SUCCESS",
                Map.of("walletId", walletId, "amount", req.amount()));
        return txn;
    }

    // FREEZE-only admin can freeze the user wallet
    @Transactional
    public WalletResponse freezeWallet(Long walletId, String role) {
        log.debug("Freeze: walletId={}, role={}", walletId, role);
        Wallet wallet = lockWallet(walletId);

        // ADMIN can ONLY freeze user wallet
        if ("USER".equals(role)) {
            throw new DeniedAccessException("ADMIN can ONLY freeze user wallet:" + walletId);
        }

        if (wallet.getStatus() == WalletStatus.CLOSED) {
            throw new WalletStatusTransitionException(
                    walletId, WalletStatus.CLOSED, WalletStatus.FROZEN);
        }

        // Idempotent behavior-if the wallet status is already frozen simply return wallet
        if (wallet.getStatus() == WalletStatus.FROZEN) {
            return walletMapper.toResponse(wallet);
        }
        wallet.setStatus(WalletStatus.FROZEN);
        walletRepo.save(wallet);
        log.info("Wallet frozen by admin: walletId={}", walletId);
        notify(wallet.getUserId(),
                "WALLET_FROZEN",
                Map.of("walletId", walletId));
        return walletMapper.toResponse(wallet);
    }

    // UNFREEZE-only admin can unfreeze the user wallet
    @Transactional
    public WalletResponse unfreezeWallet(Long walletId, String role) {
        log.debug("Unfreeze: walletId={}", walletId);
        Wallet wallet = lockWallet(walletId);


        if (role.equals("USER")) {
            throw new DeniedAccessException("ADMIN can ONLY unfreeze user wallet:" + walletId);
        }

        if (wallet.getStatus() == WalletStatus.CLOSED)
            throw new WalletStatusTransitionException(
                    walletId, WalletStatus.CLOSED, WalletStatus.ACTIVE);

        //if the wallet status is already active then return it instead of unfreezing it again
        if (wallet.getStatus() == WalletStatus.ACTIVE)
            return walletMapper.toResponse(wallet);

        wallet.setStatus(WalletStatus.ACTIVE);
        walletRepo.save(wallet);
        log.info("Wallet unfrozen by admin: walletId={}", walletId);

        notify(wallet.getUserId(), "WALLET_UNFROZEN",
                Map.of("walletId", walletId));

        return walletMapper.toResponse(wallet);
    }

    // CLOSE-Both user and admin can close the wallet
    @Transactional
    public WalletResponse closeWallet(Long walletId, Long userId, String role) {
        log.debug("Close: walletId={}", walletId);
        Wallet wallet = lockWallet(walletId);
        if (role.equals("USER") && !userId.equals(wallet.getUserId())) {
            throw new DeniedAccessException("USER cannot close another USER wallet:" + walletId);
        }

        if (wallet.getStatus() == WalletStatus.CLOSED)
            return walletMapper.toResponse(wallet);
        if (wallet.getAvailableBalance().compareTo(BigDecimal.ZERO) > 0)
            throw new BusinessRuleException("WALLET_HAS_BALANCE",
                    "Wallet " + walletId + " has balance "
                            + wallet.getAvailableBalance() + ". Withdraw first.");
        if (wallet.getHeldBalance().compareTo(BigDecimal.ZERO) > 0)
            throw new BusinessRuleException("WALLET_HAS_HELD_BALANCE",
                    "Wallet " + walletId + " has held balance "
                            + wallet.getHeldBalance() + ". Wait for pending txns.");

        wallet.setStatus(WalletStatus.CLOSED);
        walletRepo.save(wallet);
        log.info("Wallet closed: id={}", walletId);

        notify(wallet.getUserId(), "WALLET_CLOSED",
                Map.of("walletId", walletId));
        return walletMapper.toResponse(wallet);
    }

    // GET ALL WALLETS — admin only, optional status filter
    @Transactional(readOnly = true)
    public List<WalletResponse> getAllWallets(String status) {
        log.debug("Admin: get all wallets, status filter={}", status);

        List<Wallet> wallets;
        if (status == null || status.isBlank()) {
            wallets = walletRepo.findAll();
        } else {
            WalletStatus parsed;
            try {
                parsed = WalletStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessRuleException("INVALID_STATUS",
                        "Unknown wallet status: " + status
                                + ". Expected one of: ACTIVE, FROZEN, CLOSED.");
            }
            wallets = walletRepo.findByStatus(parsed);
        }

        return wallets.stream()
                .map(walletMapper::toResponse)
                .toList();
    }

    // SUMMARY — admin dashboard stat cards
    @Transactional(readOnly = true)
    public WalletSummaryResponse getSummary() {
        log.debug("Admin: get wallet summary");
        long active = walletRepo.countByStatus(WalletStatus.ACTIVE);
        long frozen = walletRepo.countByStatus(WalletStatus.FROZEN);
        long closed = walletRepo.countByStatus(WalletStatus.CLOSED);
        return new WalletSummaryResponse(active + frozen + closed, active, frozen, closed);
    }

    // GET BY WALLET ID
    @Transactional(readOnly = true)
    public WalletResponse getWalletById(Long walletId, Long userId, String role) {
        log.debug("Get wallet: id={}, userId={}, role={}", walletId, userId, role);

        Wallet wallet = walletRepo.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));

        if ("USER".equals(role) && !wallet.getUserId().equals(userId)) {
            throw new DeniedAccessException("user cannot access another user wallet:" + walletId
            );
        }

        return walletMapper.toResponse(wallet);
    }

    //GET BY USER ID
    @Transactional(readOnly = true)
    public WalletResponse getWalletByUserId(Long userId) {
        log.debug("Get wallet by userId={}", userId);
        return walletMapper.toResponse(
                walletRepo.findByUserId(userId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "No wallet found for userId: " + userId)));
    }

    //GET BALANCE BY WALLET ID
    @Transactional(readOnly = true)
    public WalletResponse getBalance(Long walletId, Long userId, String role) {

        Wallet wallet = walletRepo.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));

        // USER → only their wallet
        if ("USER".equals(role)) {
            if (!wallet.getUserId().equals(userId)) {
                throw new DeniedAccessException("user cannot view balance of another user wallet:" + walletId);
            }
        }

        return walletMapper.toResponse(wallet);
    }

    // PRIVATE -EXTERNAL
    private TransactionResponse recordTransaction(Long walletId,
                                                  Long userId,
                                                  String username,
                                                  TransactionType type,
                                                  BigDecimal amount,
                                                  String currency,
                                                  Long counterPartyUserId,
                                                  Long counterpartyWalletId,
                                                  String counterpartyUsername,
                                                  String idempotencyKey) {

        log.debug("Recording txn: walletId={}, type={}, amount={}",
                walletId, type, amount);
        try {

            CreateTransactionRequest requestBody = CreateTransactionRequest
                    .builder()
                    .username(username)
                    .walletId(walletId)
                    .type(type)
                    .amount(amount)
                    .currency(currency)
                    .idempotencyKey(idempotencyKey)
                    .build();

            if (counterpartyWalletId != null) {
                requestBody.setTargetUserId(counterPartyUserId);
                requestBody.setTargetWalletId(counterpartyWalletId);
                requestBody.setTargetUsername(counterpartyUsername);
            }



            TransactionResponse response = transactionClient.createTransaction(requestBody, userId, "USER");

            if (response == null)
                throw new ExternalServiceException(
                        "Transaction Service", "Empty response");

            log.info("Txn recorded: id={}, key={}",
                    response.getTransactionId(), response.getIdempotencyKey());

            return response;
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException("Transaction Service", e.getMessage());
        }
    }

    // NOTIFY
    private void notify(Long userId, String template, Map<String, Object> payload) {

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize notification payload: {}", e.getMessage());
            return;
        }
        SendNotificationRequest req = new SendNotificationRequest(userId, NotificationChannel.EMAIL, template, jsonPayload);
        try {
            notificationClient.send(req);
        } catch (Exception e) {
            log.warn("Notification failed: userId={}, template={}, err={}",
                    userId, template, e.getMessage());
        }
    }

    // PRIVATE — VALIDATION
    private Wallet lockWallet(Long id) {
        return walletRepo.findByIdWithLock(id)
                .orElseThrow(() -> new WalletNotFoundException(id));
    }

    private void validateSupportedCurrency(String currency) {
        try {
            SupportedCurrency.fromCode(currency);
        } catch (IllegalArgumentException e) {
            throw new UnsupportedCurrencyException(currency);
        }
    }

    private void validateOperationCurrency(String walletCurrency,
                                           String requestCurrency) {
        if (!"INR".equalsIgnoreCase(requestCurrency)) {
            throw new UnsupportedCurrencyException(requestCurrency);
        }
        if (!walletCurrency.equalsIgnoreCase(requestCurrency)) {
            throw new CurrencyMismatchException(walletCurrency, requestCurrency);
        }
    }

    //checking whether the account is active or not
    private void validateActive(Wallet w) {
        switch (w.getStatus()) {
            case ACTIVE -> {
            }
            case FROZEN -> {
                String username = resolveUsername(w.getUserId());
                throw username != null
                        ? new WalletFrozenException(username)
                        : new WalletFrozenException(w.getId());
            }
            case CLOSED -> {
                String username = resolveUsername(w.getUserId());
                throw username != null
                        ? new WalletClosedException(username)
                        : new WalletClosedException(w.getId());
            }
        }
    }

    // Resolves a wallet owner's username via USER-SERVICE for friendlier error
    // messages. Returns null on any failure so the caller can fall back to the
    // wallet ID — we never want USER-SERVICE being briefly down to break a
    // topup/transfer/withdraw with an obscure error.
    private String resolveUsername(Long userId) {
        try {
            UserLookupResponse lookup = userServiceClient.getUserByUserId(userId);
            return lookup != null ? lookup.getUsername() : null;
        } catch (Exception e) {
            log.warn("Could not resolve username for userId={}: {}", userId, e.getMessage());
            return null;
        }
    }

    //if the amount exceeds the max limit throws invalid amount exception
    private void validateAmountLimit(BigDecimal amount,
                                     BigDecimal max,
                                     String op, String currency) {
        if (amount.compareTo(max) > 0)
            throw new InvalidAmountException(amount,
                    "Max " + op + " amount for " + currency + " is " + max + ".");
    }

}