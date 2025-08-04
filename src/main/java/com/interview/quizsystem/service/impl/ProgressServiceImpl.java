package com.interview.quizsystem.service.impl;

import com.interview.quizsystem.config.LearningCycleConfig;
import com.interview.quizsystem.model.Difficulty;
import com.interview.quizsystem.model.entity.DifficultyProgress;
import com.interview.quizsystem.model.entity.Topic;
import com.interview.quizsystem.model.entity.TopicProgress;
import com.interview.quizsystem.model.entity.User;
import com.interview.quizsystem.repository.DifficultyProgressRepository;
import com.interview.quizsystem.repository.TopicProgressRepository;
import com.interview.quizsystem.service.ProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressServiceImpl implements ProgressService {

    private final TopicProgressRepository topicProgressRepository;
    private final DifficultyProgressRepository difficultyProgressRepository;
    private final LearningCycleConfig learningCycleConfig;

    @Override
    @Transactional // Changed from readOnly to allow write
    public TopicProgress getOrCreateTopicProgress(User user, Topic topic) {
        return topicProgressRepository.findByUserAndTopic(user, topic)
                .orElseGet(() -> {
                    TopicProgress progress = TopicProgress.builder()
                            .user(user)
                            .topic(topic)
                            .active(true)
                            .startDate(LocalDateTime.now())
                            .build();
                    return topicProgressRepository.save(progress);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopicProgress> getUserProgress(User user) {
        return topicProgressRepository.findByUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopicProgress> getActiveProgressByUser(User user) {
        return topicProgressRepository.findActiveProgressByUser(user);
    }

    @Override
    @Transactional
    public void updateProgress(User user, Topic topic, Difficulty difficulty, boolean isCorrect) {
        TopicProgress topicProgress = getOrCreateTopicProgress(user, topic);
        DifficultyProgress difficultyProgress = getDifficultyProgress(topicProgress, difficulty);

        // Update difficulty progress
        difficultyProgress.setQuestionsAttempted(difficultyProgress.getQuestionsAttempted() + 1);
        if (isCorrect) {
            difficultyProgress.setQuestionsCorrect(difficultyProgress.getQuestionsCorrect() + 1);
        }
        difficultyProgress.setScore(calculateScore(difficultyProgress));
        difficultyProgress.setLastAttemptDate(LocalDateTime.now());

        // Update topic progress
        topicProgress.setQuestionsAttempted(topicProgress.getQuestionsAttempted() + 1);
        if (isCorrect) {
            topicProgress.setQuestionsCorrect(topicProgress.getQuestionsCorrect() + 1);
        }
        topicProgress.setOverallScore(calculateOverallScore(topicProgress));
        topicProgress.setLastAttemptDate(LocalDateTime.now());

        // Save updates
        difficultyProgressRepository.save(difficultyProgress);
        topicProgressRepository.save(topicProgress);
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 0 * * *") // Run at midnight every day
    public void resetExpiredProgress() {
        LocalDateTime expiryDate = LocalDateTime.now().minusDays(learningCycleConfig.getDays());
        List<TopicProgress> expiredProgress = topicProgressRepository.findExpiredProgress(expiryDate);
        
        expiredProgress.forEach(this::resetProgress);
        log.info("Reset {} expired progress records", expiredProgress.size());
    }

    @Override
    @Transactional
    public void resetProgress(TopicProgress progress) {
        // Reset topic progress
        progress.setActive(false);
        
        // Create new progress entry
        TopicProgress newProgress = TopicProgress.builder()
                .user(progress.getUser())
                .topic(progress.getTopic())
                .active(true)
                .startDate(LocalDateTime.now())
                .build();
        
        topicProgressRepository.save(progress);
        topicProgressRepository.save(newProgress);
    }

    @Override
    @Transactional
    public DifficultyProgress getDifficultyProgress(TopicProgress topicProgress, Difficulty difficulty) {
        return difficultyProgressRepository.findByTopicProgressAndDifficulty(topicProgress, difficulty)
                .orElseGet(() -> {
                    DifficultyProgress progress = DifficultyProgress.builder()
                            .topicProgress(topicProgress)
                            .difficulty(difficulty)
                            .build();
                    return difficultyProgressRepository.save(progress);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean shouldProgressToDifficulty(User user, Topic topic, Difficulty targetDifficulty) {
        TopicProgress progress = getOrCreateTopicProgress(user, topic);
        
        // Check if current difficulty is mastered
        Difficulty currentDifficulty = getCurrentDifficulty(progress);
        DifficultyProgress currentProgress = getDifficultyProgress(progress, currentDifficulty);
        
        return currentProgress.getQuestionsAttempted() >= learningCycleConfig.getMinQuestions() &&
               currentProgress.getScore() >= learningCycleConfig.getDifficultyProgressionThreshold();
    }

    @Override
    @Transactional(readOnly = true)
    public double getTopicCompletionPercentage(User user, Topic topic) {
        TopicProgress progress = getOrCreateTopicProgress(user, topic);
        return progress.getOverallScore();
    }

    @Override
    @Transactional(readOnly = true)
    public double getDifficultyCompletionPercentage(User user, Topic topic, Difficulty difficulty) {
        TopicProgress progress = getOrCreateTopicProgress(user, topic);
        DifficultyProgress difficultyProgress = getDifficultyProgress(progress, difficulty);
        return difficultyProgress.getScore();
    }

    private double calculateScore(DifficultyProgress progress) {
        if (progress.getQuestionsAttempted() == 0) {
            return 0.0;
        }
        return (double) progress.getQuestionsCorrect() / progress.getQuestionsAttempted() * 100;
    }

    private double calculateOverallScore(TopicProgress progress) {
        if (progress.getQuestionsAttempted() == 0) {
            return 0.0;
        }
        return (double) progress.getQuestionsCorrect() / progress.getQuestionsAttempted() * 100;
    }

    private Difficulty getCurrentDifficulty(TopicProgress progress) {
        // Find the highest difficulty with sufficient progress
        for (Difficulty difficulty : Difficulty.values()) {
            DifficultyProgress difficultyProgress = getDifficultyProgress(progress, difficulty);
            if (difficultyProgress.getQuestionsAttempted() < learningCycleConfig.getMinQuestions() ||
                difficultyProgress.getScore() < learningCycleConfig.getDifficultyProgressionThreshold()) {
                return difficulty;
            }
        }
        return Difficulty.HARD; // All difficulties mastered
    }
} 