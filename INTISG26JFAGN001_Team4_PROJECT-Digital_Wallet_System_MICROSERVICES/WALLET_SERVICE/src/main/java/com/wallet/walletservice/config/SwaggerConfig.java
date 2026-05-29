package com.wallet.walletservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI walletServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Wallet Service API")
                        .description("One wallet per user — " +
                                "creation, top-up, transfer, withdrawal, " +
                                "freeze/unfreeze/close. " +
                                "External services (User, KYC, Transaction, Notification) " +
                                "are currently mocked and will be replaced " +
                                "when real services are available.")
                        .version("1.0.0")
                        .contact(new Contact().name("Wallet Team")))
                .servers(List.of(
                        new Server().url("http://localhost:8082")
                                .description("Local")));
    }
}