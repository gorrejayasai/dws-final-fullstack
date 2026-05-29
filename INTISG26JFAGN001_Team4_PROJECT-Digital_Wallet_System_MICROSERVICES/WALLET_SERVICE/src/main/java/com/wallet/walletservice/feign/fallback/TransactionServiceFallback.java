package com.wallet.walletservice.feign.fallback;

import com.wallet.walletservice.dto.request.CreateTransactionRequest;
import com.wallet.walletservice.dto.response.TransactionResponse;
import com.wallet.walletservice.exception.ExternalServiceException;
import com.wallet.walletservice.feign.TransactionServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class TransactionServiceFallback implements TransactionServiceClient {

    @Override
    public TransactionResponse createTransaction(CreateTransactionRequest request, Long userId, String userRole) {
        log.error("Transaction Service fallback triggered");
        throw new ExternalServiceException("Transaction Service", "Service is unreachable");
    }
}