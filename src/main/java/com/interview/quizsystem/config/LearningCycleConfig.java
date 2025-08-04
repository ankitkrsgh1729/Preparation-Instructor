package com.interview.quizsystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "learning.cycle")
public class LearningCycleConfig {
    private int days = 10;
    private int minQuestions = 20;
    private double passThreshold = 70.0;
    private double difficultyProgressionThreshold = 80.0;
} 