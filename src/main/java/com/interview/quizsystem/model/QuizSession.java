package com.interview.quizsystem.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class QuizSession {
    private String id;
    private List<Question> questions;
    private List<Question> visibleQuestions;
    private List<UserAnswer> answers;
    private String topic;
    private Difficulty difficulty;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private SessionStatus status;
    private double score;
} 