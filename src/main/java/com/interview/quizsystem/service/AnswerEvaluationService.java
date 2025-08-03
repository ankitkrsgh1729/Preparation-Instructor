package com.interview.quizsystem.service;

import com.interview.quizsystem.dto.AnswerFeedback;
import com.interview.quizsystem.model.Question;

public interface AnswerEvaluationService {
    AnswerFeedback evaluateAnswer(Question question, String userAnswer);
} 