package com.interview.quizsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicProgressDTO {
    private String topicName;
    private double overallScore;
    private int questionsAttempted;
    private int questionsCorrect;
    private LocalDateTime startDate;
    private LocalDateTime lastAttemptDate;
    private boolean active;
    private int daysRemaining;
    private List<DifficultyProgressDTO> difficultyProgress;
} 