package com.cognizant.TransactionService.controller;

import com.cognizant.TransactionService.dto.CreateTransactionRequest;
import com.cognizant.TransactionService.dto.PaginatedResponse;
import com.cognizant.TransactionService.dto.TransactionResponse;
import com.cognizant.TransactionService.dto.TransactionSummaryResponse;
import com.cognizant.TransactionService.entity.enums.TransactionStatus;
import com.cognizant.TransactionService.entity.enums.TransactionType;
import com.cognizant.TransactionService.entity.enums.UserRole;
import com.cognizant.TransactionService.exception.DeniedAccessException;
import com.cognizant.TransactionService.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/transactions")
@AllArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    // ----------------------------------------------------------------
    // INTERNAL — called by Wallet Service
    // ----------------------------------------------------------------

    @Tag(name = "Internal")
    @PostMapping("/createTransaction")
    @Operation(
            summary = "Create a transaction",
            description = "Called internally by Wallet Service to record a TOPUP, WITHDRAW, or TRANSFER. " +
                          "Idempotent — duplicate idempotency keys return the existing transaction."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transaction created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request — validation failure or business rule violation", content = @Content),
            @ApiResponse(responseCode = "409", description = "Duplicate idempotency key", content = @Content)
    })
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        log.info("Creating transaction: type={}, walletId={}, userId={}", request.getType(), request.getWalletId(), userId);
        TransactionResponse response = transactionService.createTransaction(request, userId);
        log.info("Transaction created: txnId={}", response.getTransactionId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ----------------------------------------------------------------
    // USER | ADMIN
    // ----------------------------------------------------------------

    @Tag(name = "User")
    @Tag(name = "Admin")
    @GetMapping("/{transactionId}")
    @Operation(
            summary = "Get transaction by ID",
            description = "Fetch a single transaction by its business transaction ID. " +
                          "USER role can only access their own transactions. ADMIN can access any."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction found"),
            @ApiResponse(responseCode = "403", description = "Access denied — not owner and not admin", content = @Content),
            @ApiResponse(responseCode = "404", description = "Transaction not found", content = @Content)
    })
    public ResponseEntity<TransactionResponse> getByTransactionId(
            @Parameter(description = "Business transaction ID (e.g. txn-3f8a9c2e4d1b)")
            @PathVariable String transactionId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") UserRole role
    ) {
        log.info("Fetching transaction: txnId={}, requestedBy=userId={}, role={}", transactionId, userId, role);
        return ResponseEntity.ok(transactionService.getByTransactionId(transactionId, userId, role));
    }

    // ----------------------------------------------------------------
    // USER ENDPOINTS
    // ----------------------------------------------------------------

    @Tag(name = "User")
    @GetMapping("/users/me")
    @Operation(
            summary = "List my transactions",
            description = "Returns a paginated list of all transactions for the authenticated user across all wallets. " +
                    "Optionally filter by `status` (PENDING | COMPLETED | FAILED) and/or `type` (TOPUP | WITHDRAW | TRANSFER)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of transactions")
    })
    public ResponseEntity<PaginatedResponse<TransactionResponse>> listMyTransactions(
            @Parameter(description = "Filter by status")       @RequestParam(required = false) TransactionStatus status,
            @Parameter(description = "Filter by type")         @RequestParam(required = false) TransactionType type,
            @Parameter(description = "Page number (0-based)")  @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")              @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-User-Id") Long userId
    ) {
        log.info("Listing all transactions for userId={}, status={}, type={}, page={}, size={}", userId, status, type, page, size);
        return ResponseEntity.ok(transactionService.getByUserId(userId, status, type, page, size));
    }

    @Tag(name = "User")
    @GetMapping
    @Operation(
            summary = "List transactions by wallet",
            description = "Returns a paginated list of transactions for a specific wallet. " +
                          "Optionally filter by `status` (PENDING | COMPLETED | FAILED) and/or `type` (TOPUP | WITHDRAW | TRANSFER). " +
                          "Results are always scoped to the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of transactions"),
            @ApiResponse(responseCode = "400", description = "Missing required walletId parameter", content = @Content)
    })
    public ResponseEntity<PaginatedResponse<TransactionResponse>> listTransactions(
            @Parameter(description = "ID of the wallet to query", required = true) @RequestParam Long walletId,
            @Parameter(description = "Filter by status")       @RequestParam(required = false) TransactionStatus status,
            @Parameter(description = "Filter by type")         @RequestParam(required = false) TransactionType type,
            @Parameter(description = "Page number (0-based)")  @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")              @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-User-Id") Long userId
    ) {
        log.info("Listing wallet transactions: walletId={}, userId={}, status={}, type={}, page={}, size={}",
                walletId, userId, status, type, page, size);
        return ResponseEntity.ok(
                transactionService.getByWalletIdWithFilters(walletId, userId, status, type, page, size)
        );
    }

    // ----------------------------------------------------------------
    // ADMIN ENDPOINTS
    // ----------------------------------------------------------------

    @Tag(name = "Admin")
    @GetMapping("/admin/users/{targetUserId}")
    @Operation(
            summary = "[ADMIN] List transactions by user ID",
            description = "Admin-only. Returns all transactions for a target user, optionally filtered by status and/or type. " +
                          "Not scoped to the admin's own user — fetches the target user's data. Requires `X-User-Role: ADMIN`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of transactions for the target user"),
            @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content)
    })
    public ResponseEntity<PaginatedResponse<TransactionResponse>> listTransactionsByUserIdAsAdmin(
            @Parameter(description = "ID of the target user whose transactions to retrieve", required = true)
            @PathVariable Long targetUserId,
            @Parameter(description = "Filter by status")       @RequestParam(required = false) TransactionStatus status,
            @Parameter(description = "Filter by type")         @RequestParam(required = false) TransactionType type,
            @Parameter(description = "Page number (0-based)")  @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")              @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-User-Role") UserRole role,
            @RequestHeader("X-User-Id") Long adminUserId
    ) {
        ensureAdmin(role);
        log.info("[ADMIN] Listing transactions for targetUserId={}, requestedBy=adminId={}, status={}, type={}",
                targetUserId, adminUserId, status, type);
        return ResponseEntity.ok(
                transactionService.getAdminByUserId(targetUserId, status, type, page, size)
        );
    }

    @Tag(name = "Admin")
    @GetMapping("/admin/wallets/{walletId}")
    @Operation(
            summary = "[ADMIN] List transactions by wallet ID",
            description = "Admin-only. Returns all transactions for a specific wallet, optionally filtered by status and/or type. " +
                          "Not restricted to any user — all transactions for that wallet are returned. Requires `X-User-Role: ADMIN`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of transactions for the wallet"),
            @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content)
    })
    public ResponseEntity<PaginatedResponse<TransactionResponse>> listTransactionsByWalletIdAsAdmin(
            @Parameter(description = "ID of the wallet to query", required = true) @PathVariable Long walletId,
            @Parameter(description = "Filter by status")       @RequestParam(required = false) TransactionStatus status,
            @Parameter(description = "Filter by type")         @RequestParam(required = false) TransactionType type,
            @Parameter(description = "Page number (0-based)")  @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")              @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-User-Role") UserRole role,
            @RequestHeader("X-User-Id") Long adminUserId
    ) {
        ensureAdmin(role);
        log.info("[ADMIN] Listing transactions for walletId={}, requestedBy=adminId={}, status={}, type={}",
                walletId, adminUserId, status, type);
        return ResponseEntity.ok(
                transactionService.getAdminByWalletId(walletId, status, type, page, size)
        );
    }

    @Tag(name = "Internal")
    @PatchMapping("/{transactionId}/{status}")
    @Operation(
            summary = "Update transaction status",
            description = "Transitions a PENDING transaction to COMPLETED or FAILED. " +
                          "Once COMPLETED, double-entry ledger entries are created. " +
                          "Non-PENDING transactions cannot be updated. Cannot transition back to PENDING."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Transaction is not in PENDING state or invalid target status", content = @Content),
            @ApiResponse(responseCode = "404", description = "Transaction not found", content = @Content)
    })
    public ResponseEntity<TransactionResponse> updateStatus(
            @Parameter(description = "Business transaction ID")                @PathVariable String transactionId,
            @Parameter(description = "Target status: COMPLETED or FAILED")     @PathVariable TransactionStatus status,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        log.info("Updating transaction status: txnId={}, targetStatus={}, requestedBy=userId={}", transactionId, status, userId);
        TransactionResponse response = transactionService.updateStatus(transactionId, status);
        log.info("Transaction status updated: txnId={}, newStatus={}", transactionId, response.getStatus());
        return ResponseEntity.ok(response);
    }

    @Tag(name = "User")
    @GetMapping("/users/me/summary")
    @Operation(
            summary = "Get my transaction summary",
            description = "Returns aggregate stats for the authenticated user's wallet — " +
                    "balance, topup/withdraw/transfer totals, and net flow. Intended for dashboard display."
    )
    @ApiResponse(responseCode = "200", description = "Summary returned successfully")
    public ResponseEntity<TransactionSummaryResponse> getMyTransactionSummary(
            @RequestHeader("X-User-Id") Long userId
    ) {
        log.info("Fetching transaction summary for userId={}", userId);
        return ResponseEntity.ok(transactionService.getSummaryByUserId(userId));
    }

    @Tag(name = "Admin")
    @GetMapping("/admin/all")
    @Operation(summary = "[ADMIN] List all transactions", description = "Returns all transactions across all users and wallets. Requires ADMIN role.")
    public ResponseEntity<PaginatedResponse<TransactionResponse>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("X-User-Role") UserRole role
    ) {
        ensureAdmin(role);
        log.info("[ADMIN] Listing all transactions, page={}, size={}", page, size);
        return ResponseEntity.ok(transactionService.getAllTransactions(page, size));
    }

    private void ensureAdmin(UserRole role) {
        if (role != UserRole.ADMIN) {
            throw new DeniedAccessException("ACCESS_DENIED_INSUFFICIENT_ROLE");
        }
    }

}
