package com.interview.quizsystem.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnswerFeedback {
    private boolean correct;
    private double similarityScore; // 0-100%
    private String feedback;
    private String correctParts;
    private String incorrectParts;
    private String improvementSuggestions;
    private String correctAnswer;
    private String conceptualUnderstanding;
} 