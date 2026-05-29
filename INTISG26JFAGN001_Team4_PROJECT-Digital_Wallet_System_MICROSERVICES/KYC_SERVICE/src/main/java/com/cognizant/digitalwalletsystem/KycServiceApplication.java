package com.cognizant.digitalwalletsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;


//KYC Service of Digital Wallet System runs on port 8082
// Calls other required services directly through OpenFeign.

@SpringBootApplication
@EnableFeignClients
public class KycServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(KycServiceApplication.class, args);
    }
}