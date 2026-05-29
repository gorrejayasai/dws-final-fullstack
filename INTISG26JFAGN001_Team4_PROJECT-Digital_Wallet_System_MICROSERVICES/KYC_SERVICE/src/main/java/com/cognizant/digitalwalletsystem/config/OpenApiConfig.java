package com.cognizant.digitalwalletsystem.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

//Open-API Configuration for swagger documentation
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8083}")
    private int serverPort;

    @Bean
    public OpenAPI kycServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("KYC Service API")
                        .description("""
                                Know Your Customer (KYC) microservice for the Digital Wallet System.

                                Handles:
                                - KYC submission by registered users (with User Service verification)
                                - Admin review — approve / reject with remarks
                                - Document management (Aadhaar / PAN + Address Proof)
                                """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Cognizant Digital Wallet Team")
                                .email("digitalwallet@cognizant.com"))
                        .license(new License().name("Internal Use Only")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server")
                ));
    }
}