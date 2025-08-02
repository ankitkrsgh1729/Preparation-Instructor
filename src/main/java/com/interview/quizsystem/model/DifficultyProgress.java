package com.interview.quizsystem.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DifficultyProgress {
    private int questionsAnswered;
    private int correctAnswers;
    private double accuracy;
} 