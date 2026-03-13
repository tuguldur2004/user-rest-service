package com.example.userrest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the User Profile REST Service.
 *
 * Architecture role:
 * - Exposes a REST (JSON) API for user profile CRUD.
 * - Does NOT handle authentication directly; every protected request is
 * forwarded to the SOAP Auth Service (user-soap-service) for token
 * validation before the request is allowed through.
 */
@SpringBootApplication
public class UserRestServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserRestServiceApplication.class, args);
    }
}
