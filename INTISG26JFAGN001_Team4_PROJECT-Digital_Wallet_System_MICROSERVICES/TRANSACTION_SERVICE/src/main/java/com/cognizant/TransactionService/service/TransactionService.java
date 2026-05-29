package com.cognizant.TransactionService.service;

import com.cognizant.TransactionService.dto.CreateTransactionRequest;
import com.cognizant.TransactionService.dto.PaginatedResponse;
import com.cognizant.TransactionService.dto.TransactionResponse;
import com.cognizant.TransactionService.dto.TransactionSummaryResponse;
import com.cognizant.TransactionService.entity.enums.TransactionStatus;
import com.cognizant.TransactionService.entity.enums.TransactionType;
import com.cognizant.TransactionService.entity.enums.UserRole;

public interface TransactionService {

    TransactionResponse createTransaction(CreateTransactionRequest request, Long userId);

    TransactionResponse getByTransactionId(String transactionId, Long userId, UserRole role);

    TransactionResponse getById(Long id, Long userId);

    PaginatedResponse<TransactionResponse> getByWalletIdWithFilters(
            Long walletId, Long userId, TransactionStatus status,
            TransactionType type, int page, int size);

    PaginatedResponse<TransactionResponse> getByUserId(
            Long userId, TransactionStatus status, TransactionType type, int page, int size);

    PaginatedResponse<TransactionResponse> getAdminByUserId(
            Long targetUserId, TransactionStatus status, TransactionType type, int page, int size);

    PaginatedResponse<TransactionResponse> getAdminByWalletId(
            Long walletId, TransactionStatus status, TransactionType type, int page, int size);

    TransactionResponse updateStatus(String transactionId, TransactionStatus status);

    TransactionSummaryResponse getSummaryByUserId(Long userId);

    PaginatedResponse<TransactionResponse> getAllTransactions(int page, int size);
}
