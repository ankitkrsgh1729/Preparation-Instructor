package com.interview.quizsystem.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import kong.unirest.Unirest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class OpenAIConfig {
    
    @Value("${openai.api.key}")
    private String apiKey;

    @PostConstruct
    public void init() {
        Unirest.config()
            .setDefaultHeader("Authorization", "Bearer " + apiKey)
            .setDefaultHeader("Content-Type", "application/json");
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
} 