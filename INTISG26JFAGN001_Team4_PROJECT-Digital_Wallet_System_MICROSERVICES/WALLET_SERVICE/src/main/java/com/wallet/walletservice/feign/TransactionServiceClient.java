package com.wallet.walletservice.feign;

import com.wallet.walletservice.dto.request.CreateTransactionRequest;
import com.wallet.walletservice.dto.response.TransactionResponse;
import com.wallet.walletservice.feign.fallback.TransactionServiceFallback;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(
        name = "TRANSACTION-SERVICE",
//        url = "${feign.client.config.transaction-service.url}",
        fallback = TransactionServiceFallback.class
)
public interface TransactionServiceClient {

    @PostMapping("/transactions/createTransaction")
    public TransactionResponse createTransaction(
            @Valid @RequestBody CreateTransactionRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    );
}

