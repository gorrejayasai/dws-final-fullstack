package com.cognizant.ApiGateway.client;

import com.cognizant.ApiGateway.dto.JwtValidationResponseDto;
import com.cognizant.ApiGateway.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserServiceFallback implements UserServiceClient{

    @Override
    public JwtValidationResponseDto validate(String headerToken) {
        log.warn("[CircuitBreaker] User-service is DOWN or slow. Returning fallback for token validation.");
        throw new ServiceUnavailableException("Auth service unavailable");
    }
}
