package com.interview.quizsystem.model;

import com.interview.quizsystem.dto.AnswerFeedback;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    private String id;
    private String content;
    private QuestionType type;
    private List<String> options;
    private String correctAnswer;
    private String explanation;
    private String topic;
    private Difficulty difficulty;
    private String sourceFile;
    private String sourceContent;
    private AnswerFeedback answerFeedback;
} 