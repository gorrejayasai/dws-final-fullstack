package com.wallet.walletservice.controller;

import com.wallet.walletservice.dto.request.*;
import com.wallet.walletservice.dto.response.*;
import com.wallet.walletservice.exception.common.ApiErrorResponse;
import com.wallet.walletservice.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallet", description = "One wallet per user")
@Slf4j
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    @Operation(summary = "Create wallet")
    @ApiResponses({
            @ApiResponse(responseCode = "201"),
            @ApiResponse(responseCode = "422",
                    content = @Content(schema = @Schema(
                            implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<WalletResponse> createWallet(
            @Valid @RequestBody CreateWalletRequest req) {
        log.info("POST /wallets — userId={}", req.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(walletService.createWallet(req));
    }

    @GetMapping("/admin/all")
    @Operation(summary = "[ADMIN] Get all wallets (optionally filtered by status)")
    public ResponseEntity<List<WalletResponse>> getAllWallets(
            @RequestHeader("X-User-Role") String role,
            @Parameter(description = "Optional status filter: ACTIVE | FROZEN | CLOSED",
                    example = "ACTIVE")
            @RequestParam(value = "status", required = false) String status) {
        log.info("GET /wallets/admin/all — status={}", status);
        return ResponseEntity.ok(walletService.getAllWallets(status));
    }

    @GetMapping("/admin/summary")
    @Operation(summary = "[ADMIN] Wallet counts grouped by status (for dashboard)")
    public ResponseEntity<WalletSummaryResponse> getWalletSummary(
            @RequestHeader("X-User-Role") String role) {
        log.info("GET /wallets/admin/summary");
        return ResponseEntity.ok(walletService.getSummary());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get wallet by wallet ID")
    public ResponseEntity<WalletResponse> getWalletById(
            @Parameter(example = "1000000000") @PathVariable Long id,@RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        log.info("GET /wallets/{}", id);
        return ResponseEntity.ok(walletService.getWalletById(id,userId,role));
    }

    @GetMapping("/by-user")
    @Operation(summary = "Get wallet by user ID")
    public ResponseEntity<WalletResponse> getWalletByUserId(
            @Parameter(example = "1001") @RequestHeader("X-User-Id") Long userId) {
        log.info("GET /wallets/by-user", userId);
        return ResponseEntity.ok(walletService.getWalletByUserId(userId));
    }


    @GetMapping("/{id}/balance")
    @Operation(summary = "Get wallet balance")
    public ResponseEntity<WalletResponse> getBalance(@Parameter(example = "1000000000")
                                                     @PathVariable Long id,
                                                     @RequestHeader("X-User-Id") Long userId,
                                                     @RequestHeader("X-User-Role") String role) {

        return ResponseEntity.ok(walletService.getBalance(id, userId, role));
    }

    @PostMapping("/{walletId}/topup")
    @Operation(summary = "Top up wallet")
    public ResponseEntity<TransactionResponse> topup(
            @Parameter(example = "1000000000") @PathVariable Long walletId,
            @Valid @RequestBody TopupRequest req,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        log.info("POST /wallets/{}/topup — amount={}, currency={}",
                walletId, req.amount(), req.currency());
        return ResponseEntity.ok(walletService.topup(walletId, req, userId, role));
    }

    @PostMapping("/{id}/transfer")
    @Operation(summary = "Transfer funds to another wallet")
    public ResponseEntity<TransactionResponse> transfer(
            @Parameter(example = "1000000000") @PathVariable Long id,
            @Valid @RequestBody TransferRequest req,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        log.info("POST /wallets/{}/transfer — toUsername={}, amount={}",
                id, req.targetUsername(), req.amount());
        return ResponseEntity.ok(walletService.transfer(id, req, userId, role));
    }

    @PostMapping("/{id}/withdraw")
    @Operation(summary = "Withdraw funds from wallet")
    public ResponseEntity<TransactionResponse> withdraw(
            @Parameter(example = "1000000000") @PathVariable Long id,
            @Valid @RequestBody WithdrawRequest req,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        log.info("POST /wallets/{}/withdraw — amount={}, currency={}",
                id, req.amount(), req.currency());
        return ResponseEntity.ok(walletService.withdraw(id, req, userId, role));
    }


    @PutMapping("/{id}/freeze")
    @Operation(summary = "Freeze wallet")
    public ResponseEntity<WalletResponse> freeze(@Parameter(example = "1000000000") @PathVariable Long id,
                                                 @RequestHeader("X-User-Role") String role) {
        log.info("PUT /wallets/{}/freeze —  role={}", id, role);
        return ResponseEntity.ok(walletService.freezeWallet(id,role));
    }

    @PutMapping("/{id}/unfreeze")
    @Operation(summary = "Unfreeze wallet")
    public ResponseEntity<WalletResponse> unfreeze(
            @Parameter(example = "1000000000") @PathVariable Long id,
            @RequestHeader("X-User-Role") String role) {
        log.info("PUT /wallets/{}/unfreeze", id);
        return ResponseEntity.ok(walletService.unfreezeWallet(id,role));
    }

    @PutMapping("/{id}/close")
    @Operation(summary = "Close wallet (permanent)")
    public ResponseEntity<WalletResponse> close(
            @Parameter(example = "1000000000") @PathVariable Long id,@RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        log.info("PUT /wallets/{}/close", id);
        return ResponseEntity.ok(walletService.closeWallet(id,userId,role));
    }
}