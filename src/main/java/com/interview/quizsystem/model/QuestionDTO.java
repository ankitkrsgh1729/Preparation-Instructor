package com.interview.quizsystem.model;

import com.interview.quizsystem.dto.AnswerFeedback;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDTO {
    private String id;
    private String content;  // Used as questionText in the entity
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