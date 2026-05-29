package com.cognizant.ApiGateway.client;

import com.cognizant.ApiGateway.dto.JwtValidationResponseDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "USER-SERVICE",
        path = "/user/auth",
        fallback = UserServiceFallback.class)
public interface UserServiceClient {

    @GetMapping("/validate")
    public JwtValidationResponseDto validate(@RequestHeader("Authorization") String headerToken);
}



