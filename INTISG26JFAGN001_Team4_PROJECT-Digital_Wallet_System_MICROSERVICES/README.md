# Digital Wallet System - Microservices

This repository contains a Spring Boot microservices-based Digital Wallet system.

## Project Overview

The system is split into independent services:

- `EUREKA_SERVICE_REGISTRY`: Service discovery (Eureka server)
- `API_GATEWAY`: Entry point + routing + JWT validation orchestration
- `USER_SERVICE`: Authentication, JWT, user profile
- `KYC_SERVICE`: KYC submission and admin approval/rejection flow
- `WALLET_SERVICE`: Wallet lifecycle and wallet operations (top-up, transfer, withdraw, freeze, close)
- `TRANSACTION_SERVICE`: Transaction creation, querying, and status updates
- `NOTIFICATION_SERVICE`: Notification delivery and query APIs

## Tech Stack

- Java 21
- Spring Boot (mixed versions across services)
- Spring Cloud (Eureka, Gateway, OpenFeign)
- Spring Data JPA
- MySQL
- Spring Security + JWT (in `USER_SERVICE`)
- Swagger/OpenAPI (`springdoc`)
- Maven

## Service Catalog

| Service | Default Port | Service Name in Config | Main Responsibility |
|---|---:|---|---|
| Eureka Registry | `8761` | `eurekaserver` | Service registration/discovery |
| API Gateway | `8081` | `API-GATEWAY` | Routes requests and aggregates Swagger docs |
| User Service | `8082` | `USER-SERVICE` | Signup/login/token validation/profile |
| KYC Service | `8083` | `kyc-service` | KYC submission and admin review |
| Wallet Service | `8084` | `wallet-service` | Wallet operations and orchestration |
| Transaction Service (dev profile) | `8085` | `TRANSACTION-SERVICE` | Transaction lifecycle |
| Notification Service | `8086` | `notification-service` | Send/query notifications |

## Architecture (High Level)

1. Client calls `API_GATEWAY`.
2. Gateway routes requests to downstream services by service ID via Eureka.
3. For protected wallet/KYC routes, gateway validates JWT by calling `USER-SERVICE`.
4. Gateway injects `X-User-Id` and `X-User-Role` headers for downstream authorization.
5. `WALLET_SERVICE` uses Feign clients to call KYC, Transaction, and Notification services.

## Current Gateway Routes

Configured in `API_GATEWAY/src/main/java/com/cognizant/ApiGateway/config/GatewayConfig.java`:

- `/api/v1/user/**` -> `USER-SERVICE`
- `/api/v1/wallets/**` -> `WALLET-SERVICE` (auth filter applied)
- `/api/v1/kyc/**` -> `KYC-SERVICE` (auth filter applied)

Swagger proxy routes are configured for all services. Business routes for transaction/notification are not currently mapped in gateway config.

## Prerequisites

- JDK 21
- Maven 3.9+
- MySQL running locally
- (Optional) MailHog/SMTP server for local notification testing

## Databases Used

From service configs:

- User: `auth_service_db_dev`
- KYC: `kyc_db`
- Wallet: `wallet_db`
- Transaction: `transaction_service_db_dev`
- Notification: `wallet_notifications`

All are configured to auto-create on startup when possible.

## Run Order (Recommended)

Start services in this order:

1. `EUREKA_SERVICE_REGISTRY`
2. `USER_SERVICE`
3. `KYC_SERVICE`
4. `WALLET_SERVICE`
5. `TRANSACTION_SERVICE`
6. `NOTIFICATION_SERVICE`
7. `API_GATEWAY`

> There is no root aggregator `pom.xml`; build/run each module separately.

## Build and Run Commands (Windows PowerShell)

### 1) Eureka

```powershell
Set-Location "C:\Users\2485806\OneDrive - Cognizant\Desktop\DWS_MICROSERVICES\EUREKA_SERVICE_REGISTRY"
.\mvnw.cmd clean spring-boot:run
```

### 2) User Service

```powershell
Set-Location "C:\Users\2485806\OneDrive - Cognizant\Desktop\DWS_MICROSERVICES\USER_SERVICE"
.\mvnw.cmd clean spring-boot:run
```

### 3) KYC Service

```powershell
Set-Location "C:\Users\2485806\OneDrive - Cognizant\Desktop\DWS_MICROSERVICES\KYC_SERVICE"
mvn clean spring-boot:run
```

### 4) Wallet Service

```powershell
Set-Location "C:\Users\2485806\OneDrive - Cognizant\Desktop\DWS_MICROSERVICES\WALLET_SERVICE"
.\mvnw.cmd clean spring-boot:run
```

### 5) Transaction Service

```powershell
Set-Location "C:\Users\2485806\OneDrive - Cognizant\Desktop\DWS_MICROSERVICES\TRANSACTION_SERVICE"
.\mvnw.cmd clean spring-boot:run
```

### 6) Notification Service

```powershell
Set-Location "C:\Users\2485806\OneDrive - Cognizant\Desktop\DWS_MICROSERVICES\NOTIFICATION_SERVICE"
mvn clean spring-boot:run
```

### 7) API Gateway

```powershell
Set-Location "C:\Users\2485806\OneDrive - Cognizant\Desktop\DWS_MICROSERVICES\API_GATEWAY"
.\mvnw.cmd clean spring-boot:run
```

## Run Tests

Run tests per service module:

```powershell
Set-Location "C:\Users\2485806\OneDrive - Cognizant\Desktop\DWS_MICROSERVICES\USER_SERVICE"
.\mvnw.cmd test
```

For modules without Maven wrapper, use:

```powershell
mvn test
```

## Swagger / API Docs

- Gateway Swagger UI: `http://localhost:8081/swagger-ui.html`
- Gateway OpenAPI JSON: `http://localhost:8081/v3/api-docs`
- Eureka dashboard: `http://localhost:8761`

## Common Troubleshooting

### 1) `user_chk_1` check constraint violated during signup

If you see errors like:

- `Check constraint 'user_chk_1' is violated`

It usually indicates enum/database constraint mismatch on `role` or `status` in the `user` table (expected enum values are from `UserRole` and `UserStatus`).

Quick checks:

1. Ensure signup payload contains valid fields (`username`, `email`, `password`).
2. Inspect existing `user` table definition and check constraints in MySQL.
3. If schema is stale from older enum values, back up data and recreate/alter table constraints.
4. Restart `USER_SERVICE` after schema fix.

### 2) Service not visible in Eureka

- Confirm Eureka is running on `8761` first.
- Check `spring.application.name` and `eureka.client.service-url.defaultZone` in each service config.

### 3) Port conflicts

- Confirm ports `8081`-`8086` and `8761` are free.
- In `TRANSACTION_SERVICE`, active profile defaults to `dev` (port `8085`).

## Notes

- The repository currently uses mixed Spring Boot/Spring Cloud versions across modules.
- Prefer running/validating services independently before full integration testing.
- `KYC_SERVICE` and `NOTIFICATION_SERVICE` currently rely on system Maven (`mvn`) because wrapper scripts are not present in those folders.

