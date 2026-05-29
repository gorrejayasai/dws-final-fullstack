package com.springbootmicroservices.eurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * This class sets up and runs a Eureka Server for service discovery. Eureka Server
 * is part of the Netflix OSS suite and provides a REST-based interface for services
 * to register and discover each other.
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaserverApplication {

	//Main method to run the Spring Boot application.
	public static void main(String[] args) {
		SpringApplication.run(EurekaserverApplication.class, args);
	}

}
