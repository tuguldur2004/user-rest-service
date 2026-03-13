package com.example.userrest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * General application beans.
 */
@Configuration
public class AppConfig {

    /**
     * Shared RestTemplate used by
     * {@link com.example.userrest.service.SoapAuthClient}
     * to communicate with the SOAP Auth Service over HTTP.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
