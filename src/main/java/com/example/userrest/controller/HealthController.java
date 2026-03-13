package com.example.userrest.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Simple health-check endpoint used by load balancers and the frontend
 * to verify the service is running.
 *
 * GET /health → { "status": "ok", "service": "user-rest-service", "port":
 * "3000" }
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "ok",
                "service", "user-rest-service",
                "port", "3000");
    }
}
