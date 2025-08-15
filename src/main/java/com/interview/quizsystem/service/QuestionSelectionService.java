package com.interview.quizsystem.service;

import com.interview.quizsystem.model.Difficulty;
import com.interview.quizsystem.model.QuestionDTO;
import com.interview.quizsystem.model.entity.Topic;
import com.interview.quizsystem.model.entity.User;

import java.util.List;

public interface QuestionSelectionService {
    
    /**
     * Select questions for a new session based on spaced repetition and difficulty progression
     */
    List<QuestionDTO> selectQuestionsForSession(User user, Topic topic, Difficulty difficulty, int questionCount);
    
    /**
     * Select questions prioritizing due spaced repetition questions
     */
    List<QuestionDTO> selectDueQuestions(User user, Topic topic, int questionCount);
    
    /**
     * Select questions based on difficulty progression (master easier before harder)
     */
    List<QuestionDTO> selectQuestionsByDifficultyProgression(User user, Topic topic, int questionCount);
    
    /**
     * Select questions based on session momentum (easier for struggling users)
     */
    List<QuestionDTO> selectQuestionsByMomentum(User user, Topic topic, String sessionId, int questionCount);
    
    /**
     * Get recommended difficulty level for a user and topic
     */
    Difficulty getRecommendedDifficulty(User user, Topic topic);
    
    /**
     * Check if user should advance to next difficulty level
     */
    boolean shouldAdvanceDifficulty(User user, Topic topic, Difficulty currentDifficulty);
    
    /**
     * Get questions for review (due spaced repetition questions)
     */
    List<QuestionDTO> getReviewQuestions(User user, Topic topic, int questionCount);
    
    /**
     * Get questions for learning (new questions at appropriate difficulty)
     */
    List<QuestionDTO> getLearningQuestions(User user, Topic topic, int questionCount);
}

