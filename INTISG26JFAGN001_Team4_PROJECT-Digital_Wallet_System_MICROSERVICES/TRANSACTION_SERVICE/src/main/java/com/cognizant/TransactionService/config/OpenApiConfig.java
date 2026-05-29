package com.cognizant.TransactionService.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI transactionServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Transaction Service API")
                        .description("""
                                Manages immutable financial transactions (TOPUP, WITHDRAW, TRANSFER) and double-entry ledger entries.
                                
                                **Authentication:** All requests must include `X-User-Id` and `X-User-Role` headers, injected by the API Gateway after JWT validation.
                                
                                **Roles:**
                                - `USER` — can access their own transactions
                                - `ADMIN` — can access any user's transactions
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("DWS Platform Team")
                                .email("platform@dws.internal"))
                        .license(new License()
                                .name("Internal Use Only")))
                .tags(List.of(
                        new Tag().name("Internal").description("Called by other microservices (e.g. Wallet Service)"),
                        new Tag().name("User").description("Endpoints accessible by authenticated users (USER role)"),
                        new Tag().name("Admin").description("Endpoints accessible only by admin users (ADMIN role)")
                ));
    }

}
