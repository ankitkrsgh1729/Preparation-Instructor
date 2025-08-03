package com.interview.quizsystem.service;

import com.interview.quizsystem.model.QuizSession;
import com.interview.quizsystem.model.Difficulty;

public interface QuizSessionService {
    QuizSession startSession(String topic, Difficulty difficulty, int questionCount);
    QuizSession submitAnswer(String sessionId, String questionId, String answer);
    QuizSession getSession(String sessionId);
    QuizSession endSession(String sessionId);
} 