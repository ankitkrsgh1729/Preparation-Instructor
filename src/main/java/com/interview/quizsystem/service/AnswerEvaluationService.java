package com.interview.quizsystem.service;

import com.interview.quizsystem.dto.AnswerFeedback;
import com.interview.quizsystem.model.QuestionDTO;

public interface AnswerEvaluationService {
    AnswerFeedback evaluateAnswer(QuestionDTO question, String userAnswer);
} 