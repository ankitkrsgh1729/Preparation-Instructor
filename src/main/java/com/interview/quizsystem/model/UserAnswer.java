package com.interview.quizsystem.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAnswer {
    private String questionId;
    private String answer;
    private boolean correct;
    private LocalDateTime answeredAt;
} 