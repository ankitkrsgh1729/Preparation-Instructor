package com.interview.quizsystem.service;

import com.interview.quizsystem.model.Difficulty;
import com.interview.quizsystem.model.entity.UserQuestionPerformance;
import com.interview.quizsystem.model.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserQuestionPerformanceService {
    
    /**
     * Record an attempt for a question
     */
    UserQuestionPerformance recordAttempt(User user, String questionId, Long topicId, Difficulty difficulty, boolean isCorrect, long responseTimeMs);
    
    /**
     * Get performance data for a specific question
     */
    Optional<UserQuestionPerformance> getPerformance(User user, String questionId);
    
    /**
     * Get all performance data for a user and topic
     */
    List<UserQuestionPerformance> getPerformanceByTopic(User user, Long topicId);
    
    /**
     * Get all performance data for a user and difficulty
     */
    List<UserQuestionPerformance> getPerformanceByDifficulty(User user, Difficulty difficulty);
    
    /**
     * Get performance data for a user, topic, and difficulty
     */
    List<UserQuestionPerformance> getPerformanceByTopicAndDifficulty(User user, Long topicId, Difficulty difficulty);
    
    /**
     * Get average response time for a user and topic
     */
    Double getAverageResponseTime(User user, Long topicId);
    
    /**
     * Get average accuracy for a user and topic
     */
    Double getAverageAccuracy(User user, Long topicId);
    
    /**
     * Check if user has achieved 70% accuracy on current difficulty level for a topic
     */
    boolean hasAchievedProficiencyLevel(User user, Long topicId, Difficulty difficulty);
    
    /**
     * Get questions where user is struggling (accuracy < 50%)
     */
    List<UserQuestionPerformance> getStrugglingQuestions(User user, Long topicId);
    
    /**
     * Get questions where user is excelling (accuracy > 80%)
     */
    List<UserQuestionPerformance> getExcellingQuestions(User user, Long topicId);
}

