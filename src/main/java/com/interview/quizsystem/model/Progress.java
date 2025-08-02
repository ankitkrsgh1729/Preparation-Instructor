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
public class Progress {
    private Map<String, TopicProgress> topicProgressMap;
    private int totalQuestionsAnswered;
    private int totalCorrectAnswers;
    private LocalDateTime lastUpdated;
} 