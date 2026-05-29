package com.wallet.walletservice.feign.fallback;

import com.wallet.walletservice.dto.response.UserLookupResponse;
import com.wallet.walletservice.exception.ExternalServiceException;
import com.wallet.walletservice.feign.UserServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserServiceFallback implements UserServiceClient {

    @Override
    public UserLookupResponse getUserByUsername(String username) {
        log.error("User Service fallback triggered for username lookup: {}", username);
        throw new ExternalServiceException("User Service", "Service is unreachable");
    }

    @Override
    public UserLookupResponse getUserByUserId(Long userId) {
        log.error("User Service fallback triggered for userId lookup: {}", userId);
        throw new ExternalServiceException("User Service", "Service is unreachable");
    }
}