package com.interview.quizsystem.service;

import com.interview.quizsystem.model.Difficulty;
import com.interview.quizsystem.model.entity.DifficultyProgress;
import com.interview.quizsystem.model.entity.Topic;
import com.interview.quizsystem.model.entity.TopicProgress;
import com.interview.quizsystem.model.entity.User;

import java.util.List;

public interface ProgressService {
    // Topic progress management
    TopicProgress getOrCreateTopicProgress(User user, Topic topic);
    List<TopicProgress> getUserProgress(User user);
    List<TopicProgress> getActiveProgressByUser(User user);
    
    // Update progress after quiz
    void updateProgress(User user, Topic topic, Difficulty difficulty, boolean isCorrect);
    
    // Progress reset
    void resetExpiredProgress();
    void resetProgress(TopicProgress progress);
    
    // Difficulty management
    DifficultyProgress getDifficultyProgress(TopicProgress topicProgress, Difficulty difficulty);
    boolean shouldProgressToDifficulty(User user, Topic topic, Difficulty targetDifficulty);
    
    // Analytics
    double getTopicCompletionPercentage(User user, Topic topic);
    double getDifficultyCompletionPercentage(User user, Topic topic, Difficulty difficulty);
} 