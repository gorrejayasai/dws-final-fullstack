package com.cognizant.ApiGateway.filter;

import com.cognizant.ApiGateway.client.UserServiceClient;
import com.cognizant.ApiGateway.dto.JwtValidationResponseDto;
import com.cognizant.ApiGateway.exception.InvalidTokenException;
import com.cognizant.ApiGateway.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {
    @Override
    public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> next) throws Exception {
        String path = request.path();
        String clientIp = extractClientIp(request);

        log.info("[Gateway] Incoming request: method={} path={} clientIp={}", request.method(), path, clientIp);

        // 1. Skip for public paths
        if(isPublicPath(path)){
            log.debug("[Gateway] Public path, skipping auth: {}", path);
            return next.handle(request);
        }

        // 2. Extract token
        String authHeader = request.headers().firstHeader(HttpHeaders.AUTHORIZATION);
        if(authHeader == null || authHeader.isBlank()){
            log.warn("[Auth] Missing Authorization header for path={}", path);
            return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                    .body("Missing Authorization header");
        }

        // 3. Validate token via Feign
        JwtValidationResponseDto validation;
        try {
            validation = userServiceClient.validate(authHeader);
        } catch (InvalidTokenException e) {
            log.warn("[Auth] Invalid token for path={}: {}", path, e.getMessage());
            return ServerResponse.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            // Service down — circuit breaker fallback returns safe object,
            // but if something else throws, fail closed
            log.error("[Auth] Auth service error for path={}: {}", path, e.getMessage());
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("Authentication service unavailable");
        }

        if (validation == null || !validation.getIsValid()) {
            log.warn("[Auth] Token invalid or validation returned null for path={}", path);
            return ServerResponse.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        log.info("[Auth] Token valid. userId={} userRole={} path={}",
                validation.getUserId(), validation.getRole(), path);

        // 4. Mutate request — remove any client-supplied headers first to prevent duplication
        ServerRequest mutatedRequest = ServerRequest.from(request)
                .headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-User-Role");
                })
                .header("X-User-Id", String.valueOf(validation.getUserId()))
                .header("X-User-Role", String.valueOf(validation.getRole()))
                .build();

        return next.handle(mutatedRequest);
    }


    private final UserServiceClient userServiceClient;

    private static final List<String> PUBLIC_PATHS = List.of(
            "/actuator",
            "/swagger",
            "/swagger-ui",
            "/v3/api-docs"
    );

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private String extractClientIp(ServerRequest request) {
        // Respect X-Forwarded-For if behind a proxy/load balancer
        String forwarded = request.headers().firstHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.remoteAddress()
                .map(addr -> addr.getAddress().getHostAddress())
                .orElse("unknown");
    }

}
