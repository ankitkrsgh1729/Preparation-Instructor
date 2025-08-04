package com.interview.quizsystem.dto;

import com.interview.quizsystem.model.Difficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DifficultyProgressDTO {
    private Difficulty difficulty;
    private double score;
    private int questionsAttempted;
    private int questionsCorrect;
    private LocalDateTime lastAttemptDate;
    private boolean isCurrentDifficulty;
    private boolean canProgress;
} 