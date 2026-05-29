package com.cognizant.digitalwalletsystem.util;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * OpenFeign declarative HTTP client that validates whether a userId is
 * registered in the User Service.
 *
 * Contract expected from User Service:
 *   GET /api/users/{userId}/exists  →  200 OK        (user exists)
 *                                   →  404 NOT FOUND (user does not exist)
 *
 * ──────────────────────────────────────────────────────────────────────────
 * Configuration:
 *   The base URL is driven by the property:
 *     app.user-service.base-url  (application.yml / application-dev.yml)
 *
 * ──────────────────────────────────────────────────────────────────────────
 * Integration note for the Assembler:
 * ──────────────────────────────────────────────────────────────────────────
 *  Standalone  : url = http://localhost:8081   (set in application.yml)
 *  With Eureka : remove the url attribute and rely on service discovery —
 *                Feign + Spring Cloud LoadBalancer will resolve "user-service"
 *                automatically via Eureka.
 * ──────────────────────────────────────────────────────────────────────────
 */
@FeignClient(name = "user-service")
public interface UserServiceClient {

    /**
     * Verifies that the given userId exists in the User Service.
     *
     * @param userId the user to validate
     * @throws feign.FeignException.NotFound if the User Service returns 404
     * @throws feign.RetryableException      if the User Service is unreachable
     */
    @GetMapping("/api/users/{userId}/exists")
    void verifyUserExists(@PathVariable("userId") Long userId);
}
