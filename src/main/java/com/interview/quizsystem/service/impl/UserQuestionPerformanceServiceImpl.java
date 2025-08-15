package com.interview.quizsystem.service.impl;

import com.interview.quizsystem.model.Difficulty;
import com.interview.quizsystem.model.entity.Topic;
import com.interview.quizsystem.model.entity.User;
import com.interview.quizsystem.model.entity.UserQuestionPerformance;
import com.interview.quizsystem.repository.TopicRepository;
import com.interview.quizsystem.repository.UserQuestionPerformanceRepository;
import com.interview.quizsystem.service.UserQuestionPerformanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserQuestionPerformanceServiceImpl implements UserQuestionPerformanceService {

    private final UserQuestionPerformanceRepository userQuestionPerformanceRepository;
    private final TopicRepository topicRepository;

    @Override
    @Transactional
    public UserQuestionPerformance recordAttempt(User user, String questionId, Long topicId, Difficulty difficulty, boolean isCorrect, long responseTimeMs) {
        log.debug("Recording attempt for user {} question {}: correct={}, responseTime={}ms", user.getId(), questionId, isCorrect, responseTimeMs);
        
        UserQuestionPerformance performance = userQuestionPerformanceRepository
            .findByUserIdAndQuestionId(user.getId(), questionId)
            .orElseGet(() -> createNewPerformance(user, questionId, topicId, difficulty));
        
        performance.recordAttempt(isCorrect, responseTimeMs);
        
        UserQuestionPerformance savedPerformance = userQuestionPerformanceRepository.save(performance);
        log.debug("Recorded attempt: total={}, correct={}, accuracy={}%", 
            savedPerformance.getTotalAttempts(), savedPerformance.getCorrectAttempts(), savedPerformance.getAccuracyRate());
        
        return savedPerformance;
    }

    private UserQuestionPerformance createNewPerformance(User user, String questionId, Long topicId, Difficulty difficulty) {
        Topic topic = topicRepository.findById(topicId)
            .orElseThrow(() -> new IllegalArgumentException("Topic not found: " + topicId));
            
        return UserQuestionPerformance.builder()
            .user(user)
            .questionId(questionId)
            .topic(topic)
            .difficulty(difficulty)
            .totalAttempts(0)
            .correctAttempts(0)
            .averageResponseTimeMs(0L)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserQuestionPerformance> getPerformance(User user, String questionId) {
        log.debug("Getting performance for user {} question {}", user.getId(), questionId);
        return userQuestionPerformanceRepository.findByUserIdAndQuestionId(user.getId(), questionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserQuestionPerformance> getPerformanceByTopic(User user, Long topicId) {
        log.debug("Getting performance for user {} topic {}", user.getId(), topicId);
        return userQuestionPerformanceRepository.findByUserIdAndTopicId(user.getId(), topicId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserQuestionPerformance> getPerformanceByDifficulty(User user, Difficulty difficulty) {
        log.debug("Getting performance for user {} difficulty {}", user.getId(), difficulty);
        return userQuestionPerformanceRepository.findByUserIdAndDifficulty(user.getId(), difficulty);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserQuestionPerformance> getPerformanceByTopicAndDifficulty(User user, Long topicId, Difficulty difficulty) {
        log.debug("Getting performance for user {} topic {} difficulty {}", user.getId(), topicId, difficulty);
        return userQuestionPerformanceRepository.findByUserIdAndTopicIdAndDifficulty(user.getId(), topicId, difficulty);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageResponseTime(User user, Long topicId) {
        log.debug("Getting average response time for user {} topic {}", user.getId(), topicId);
        return userQuestionPerformanceRepository.getAverageResponseTimeByUserAndTopic(user.getId(), topicId);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageAccuracy(User user, Long topicId) {
        log.debug("Getting average accuracy for user {} topic {}", user.getId(), topicId);
        return userQuestionPerformanceRepository.getAverageAccuracyByUserAndTopic(user.getId(), topicId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAchievedProficiencyLevel(User user, Long topicId, Difficulty difficulty) {
        log.debug("Checking proficiency level for user {} topic {} difficulty {}", user.getId(), topicId, difficulty);
        
        List<UserQuestionPerformance> performances = getPerformanceByTopicAndDifficulty(user, topicId, difficulty);
        
        if (performances.isEmpty()) {
            return false;
        }
        
        double totalAccuracy = performances.stream()
            .mapToDouble(UserQuestionPerformance::getAccuracyRate)
            .sum();
        
        double averageAccuracy = totalAccuracy / performances.size();
        
        boolean hasProficiency = averageAccuracy >= 70.0;
        log.debug("Proficiency check: average accuracy={}%, has proficiency={}", averageAccuracy, hasProficiency);
        
        return hasProficiency;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserQuestionPerformance> getStrugglingQuestions(User user, Long topicId) {
        log.debug("Getting struggling questions for user {} topic {}", user.getId(), topicId);
        
        List<UserQuestionPerformance> performances = getPerformanceByTopic(user, topicId);
        
        return performances.stream()
            .filter(p -> p.getAccuracyRate() < 50.0)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserQuestionPerformance> getExcellingQuestions(User user, Long topicId) {
        log.debug("Getting excelling questions for user {} topic {}", user.getId(), topicId);
        
        List<UserQuestionPerformance> performances = getPerformanceByTopic(user, topicId);
        
        return performances.stream()
            .filter(p -> p.getAccuracyRate() > 80.0)
            .toList();
    }
}

