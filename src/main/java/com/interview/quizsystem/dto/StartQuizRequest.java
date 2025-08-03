package com.interview.quizsystem.dto;

import com.interview.quizsystem.model.Difficulty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StartQuizRequest {
    @NotBlank(message = "Topic is required")
    private String topic;

    @NotNull(message = "Difficulty is required")
    private Difficulty difficulty;

    @Min(value = 1, message = "Question count must be at least 1")
    @Max(value = 20, message = "Question count cannot exceed 20")
    private int questionCount;
} 