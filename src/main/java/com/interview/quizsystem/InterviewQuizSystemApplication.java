package com.interview.quizsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InterviewQuizSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(InterviewQuizSystemApplication.class, args);
    }
} 