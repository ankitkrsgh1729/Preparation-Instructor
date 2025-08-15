package com.interview.quizsystem.service;

import com.interview.quizsystem.model.entity.SpacedRepetitionData;
import com.interview.quizsystem.model.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public interface SpacedRepetitionService {
    
    /**
     * Initialize spaced repetition data for a new question
     */
    SpacedRepetitionData initializeQuestion(User user, String questionId);
    
    /**
     * Process answer and update spaced repetition data
     */
    SpacedRepetitionData processAnswer(User user, String questionId, boolean isCorrect);
    
    /**
     * Get due questions for a user
     */
    List<SpacedRepetitionData> getDueQuestions(User user);
    
    /**
     * Get due questions for a user up to a specific limit
     */
    List<SpacedRepetitionData> getDueQuestions(User user, int limit);
    
    /**
     * Count due questions for a user
     */
    long countDueQuestions(User user);
    
    /**
     * Get spaced repetition data for specific questions
     */
    List<SpacedRepetitionData> getSpacedRepetitionData(User user, List<String> questionIds);
    
    /**
     * Check if a question is due for review
     */
    boolean isQuestionDue(User user, String questionId);
    
    /**
     * Get next review date for a question
     */
    LocalDateTime getNextReviewDate(User user, String questionId);
}

