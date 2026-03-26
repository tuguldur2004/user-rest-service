package com.example.userrest.config;

import com.example.userrest.filter.AuthTokenFilter;
import com.example.userrest.service.SoapAuthClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Web layer configuration:
 * 1. CORS — allows the frontend (any origin during development) to call the
 * API.
 * 2. Filter registration — plugs {@link AuthTokenFilter} into the servlet
 * pipeline
 * so it runs before every request that matches /users and /users/*.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final SoapAuthClient soapAuthClient;
    private final String[] allowedOrigins;

    public WebConfig(
            SoapAuthClient soapAuthClient,
            @Value("${app.cors.allowed-origins:http://localhost:3000}") String corsAllowedOrigins) {
        this.soapAuthClient = soapAuthClient;
        this.allowedOrigins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toArray(String[]::new);
    }

    // ── CORS ──────────────────────────────────────────────────────────────────

    /**
     * During development all origins are accepted.
     * In production replace "*" with the exact frontend origin.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins) // Changed from allowedOrigins to allowedOriginPatterns
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    // ── Auth filter ───────────────────────────────────────────────────────────

    /**
     * Registers {@link AuthTokenFilter} for the /users endpoint family.
     *
     * URL patterns covered:
     * /users and /users/* → all CRUD actions require a valid Bearer token
     */
    @Bean
    public FilterRegistrationBean<AuthTokenFilter> authTokenFilter() {
        FilterRegistrationBean<AuthTokenFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AuthTokenFilter(soapAuthClient));
        registration.addUrlPatterns("/users", "/users/*");
        registration.setOrder(1); // run before other filters
        registration.setName("authTokenFilter");
        return registration;
    }
}
