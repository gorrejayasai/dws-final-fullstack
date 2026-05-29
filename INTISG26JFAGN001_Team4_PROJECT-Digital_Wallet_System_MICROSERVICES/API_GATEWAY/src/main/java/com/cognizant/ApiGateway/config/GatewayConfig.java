package com.cognizant.ApiGateway.config;

import com.cognizant.ApiGateway.filter.AdminFilter;
import com.cognizant.ApiGateway.filter.AuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.stripPrefix;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.method;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

    private final AuthenticationFilter authenticationFilter;
    private final AdminFilter adminFilter;

    // route for user service
    @Bean
    public RouterFunction<ServerResponse> userServiceRoute() {
        return GatewayRouterFunctions.route("USER-SERVICE")
                .route(path("/api/v1/user/**"), HandlerFunctions.http())
                .before(stripPrefix(2))
                .filter(LoadBalancerFilterFunctions.lb("USER-SERVICE"))
                .build();
    }

    // route for wallet service
    @Bean
    public RouterFunction<ServerResponse> walletServiceRoute(){
        return GatewayRouterFunctions.route("WALLET-SERVICE")
                .route(path("/api/v1/wallets/**"), HandlerFunctions.http())
                .before(stripPrefix(2))
                .filter(authenticationFilter)
                .filter(LoadBalancerFilterFunctions.lb("WALLET-SERVICE"))
                .build();
    }

    // route for kyc service
    @Bean
    public RouterFunction<ServerResponse> kycServiceRoute() {
        return GatewayRouterFunctions.route("KYC-SERVICE")
                .route(path("/api/v1/kyc/**"), HandlerFunctions.http())
                .before(stripPrefix(2))
                .filter(authenticationFilter)
                .filter(LoadBalancerFilterFunctions.lb("KYC-SERVICE"))
                .build();
    }

    // route for transaction service
    @Bean
    public RouterFunction<ServerResponse> transactionServiceRoute() {
        return GatewayRouterFunctions.route("TRANSACTION-SERVICE")
                // user routes
                .route(path("/api/v1/transactions").and(method(HttpMethod.GET)), HandlerFunctions.http())
                .route(path("/api/v1/transactions/users/me").and(method(HttpMethod.GET)), HandlerFunctions.http())
                .route(path("/api/v1/transactions/{transactionId}").and(method(HttpMethod.GET)), HandlerFunctions.http())
                .route(path("/api/v1/transactions/users/me/summary").and(method(HttpMethod.GET)), HandlerFunctions.http())
                // admin routes
                .route(path("/api/v1/transactions/admin/all").and(method(HttpMethod.GET)), HandlerFunctions.http())
                .route(path("/api/v1/transactions/admin/users/{targetUserId}").and(method(HttpMethod.GET)), HandlerFunctions.http())
                .route(path("/api/v1/transactions/admin/wallets/{walletId}").and(method(HttpMethod.GET)), HandlerFunctions.http())
                .before(stripPrefix(2))
                .filter(authenticationFilter)
                .filter(adminFilter)
                .filter(LoadBalancerFilterFunctions.lb("TRANSACTION-SERVICE"))
                .build();
    }

    // route for notification service
    @Bean
    public RouterFunction<ServerResponse> notificationServiceRoute() {
        return GatewayRouterFunctions.route("NOTIFICATION-SERVICE")
                // USER routes
                .route(path("/api/v1/notify/user/{userId}").and(method(HttpMethod.GET)), HandlerFunctions.http())
                .route(path("/api/v1/notify/user/{userId}/status/{status}").and(method(HttpMethod.GET)), HandlerFunctions.http())
                .before(stripPrefix(2))
                .filter(authenticationFilter)
                .filter(LoadBalancerFilterFunctions.lb("NOTIFICATION-SERVICE"))
                .build();
    }

    // downstream OpenAPI docs routes for aggregated Swagger UI at gateway
    @Bean
    public RouterFunction<ServerResponse> userServiceDocsRoute() {
        return GatewayRouterFunctions.route("USER-SERVICE-DOCS")
                .route(path("/swagger/user/v3/api-docs"), HandlerFunctions.http())
                .before(stripPrefix(2))
                .filter(LoadBalancerFilterFunctions.lb("USER-SERVICE"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> kycServiceDocsRoute() {
        return GatewayRouterFunctions.route("KYC-SERVICE-DOCS")
                .route(path("/swagger/kyc/v3/api-docs"), HandlerFunctions.http())
                .before(stripPrefix(2))
                .filter(LoadBalancerFilterFunctions.lb("KYC-SERVICE"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> walletServiceDocsRoute() {
        return GatewayRouterFunctions.route("WALLET-SERVICE-DOCS")
                .route(path("/swagger/wallet/v3/api-docs"), HandlerFunctions.http())
                .before(stripPrefix(2))
                .filter(LoadBalancerFilterFunctions.lb("WALLET-SERVICE"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> transactionServiceDocsRoute() {
        return GatewayRouterFunctions.route("TRANSACTION-SERVICE-DOCS")
                .route(path("/swagger/transaction/v3/api-docs"), HandlerFunctions.http())
                .before(stripPrefix(2))
                .filter(LoadBalancerFilterFunctions.lb("TRANSACTION-SERVICE"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> notificationServiceDocsRoute() {
        return GatewayRouterFunctions.route("NOTIFICATION-SERVICE-DOCS")
                .route(path("/swagger/notification/v3/api-docs"), HandlerFunctions.http())
                .before(stripPrefix(2))
                .filter(LoadBalancerFilterFunctions.lb("NOTIFICATION-SERVICE"))
                .build();
    }
}
