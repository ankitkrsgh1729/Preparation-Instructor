package com.interview.quizsystem.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicProgress {
    private String topic;
    private int questionsAnswered;
    private int correctAnswers;
    private double accuracy;
    private Difficulty currentDifficulty;
    private LocalDateTime lastStudied;
    private Map<Difficulty, DifficultyProgress> difficultyProgressMap;
} 